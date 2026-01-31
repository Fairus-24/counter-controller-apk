package app.counter.controller.caba;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple HTTP server to serve assets via http:// for Firebase Auth compatibility
 */
public class LocalHttpServer {
    private static final String TAG = "LocalHttpServer";
    
    private final Context context;
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private boolean isRunning = false;
    
    // MIME types
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("htm", "text/html; charset=utf-8");
        MIME_TYPES.put("css", "text/css; charset=utf-8");
        MIME_TYPES.put("js", "application/javascript; charset=utf-8");
        MIME_TYPES.put("json", "application/json; charset=utf-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("wav", "audio/wav");
        MIME_TYPES.put("ogg", "audio/ogg");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("xml", "application/xml");
        MIME_TYPES.put("txt", "text/plain; charset=utf-8");
    }
    
    public LocalHttpServer(Context context, int port) throws IOException {
        this.context = context;
        this.port = port;
    }
    
    public void start() throws IOException {
        if (isRunning) return;
        
        serverSocket = new ServerSocket(port);
        executor = Executors.newCachedThreadPool();
        isRunning = true;
        
        new Thread(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleRequest(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "Accept error", e);
                    }
                }
            }
        }).start();
        
        Log.i(TAG, "Server started on port " + port);
    }
    
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing server", e);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
    
    private void handleRequest(Socket socket) {
        try {
            InputStream input = socket.getInputStream();
            StringBuilder requestBuilder = new StringBuilder();
            
            // Read HTTP request
            byte[] buffer = new byte[4096];
            int bytesRead = input.read(buffer);
            if (bytesRead > 0) {
                requestBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
            
            String request = requestBuilder.toString();
            String[] requestLines = request.split("\r\n");
            
            if (requestLines.length == 0) {
                socket.close();
                return;
            }
            
            // Parse request line: GET /path HTTP/1.1
            String[] requestParts = requestLines[0].split(" ");
            if (requestParts.length < 2) {
                socket.close();
                return;
            }
            
            String method = requestParts[0];
            String path = requestParts[1];
            
            // Remove query string
            int queryIndex = path.indexOf('?');
            if (queryIndex > 0) {
                path = path.substring(0, queryIndex);
            }
            
            // Remove leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // Default to index.html
            if (path.isEmpty() || path.equals("/")) {
                path = "index.html";
            }
            
            Log.d(TAG, "Request: " + method + " /" + path);
            
            // Serve asset
            byte[] content = loadAsset(path);
            String mimeType = getMimeType(path);
            
            String response;
            byte[] responseBytes;
            
            if (content != null) {
                String headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + content.length + "\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Cache-Control: no-cache\r\n" +
                    "Connection: close\r\n\r\n";
                
                // Write headers + content
                socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().write(content);
            } else {
                // 404 Not Found
                String notFound = "<!DOCTYPE html><html><body><h1>404 Not Found</h1><p>File not found: /" + path + "</p></body></html>";
                byte[] notFoundBytes = notFound.getBytes(StandardCharsets.UTF_8);
                
                String headers = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: " + notFoundBytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";
                
                socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().write(notFoundBytes);
                
                Log.w(TAG, "404 Not Found: /" + path);
            }
            
            socket.getOutputStream().flush();
            socket.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Error handling request", e);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
    
    private byte[] loadAsset(String path) {
        AssetManager assets = context.getAssets();
        
        try {
            InputStream is = assets.open(path);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            is.close();
            return baos.toByteArray();
        } catch (IOException e) {
            // Try with www/ prefix
            try {
                InputStream is = assets.open("www/" + path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                is.close();
                return baos.toByteArray();
            } catch (IOException e2) {
                return null;
            }
        }
    }
    
    private String getMimeType(String path) {
        String extension = "";
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            extension = path.substring(lastDot + 1).toLowerCase();
        }
        String mimeType = MIME_TYPES.get(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }
}
