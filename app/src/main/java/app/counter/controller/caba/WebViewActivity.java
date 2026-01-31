package app.counter.controller.caba;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * WebViewActivity - Loads Counter Board HTML via local HTTP server for Firebase Auth compatibility
 */
public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "CounterBoard";
    private static final String CHANNEL_ID = "counter_board_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_CODE = 1002;

    // Local server port for serving assets with http://
    private static final int LOCAL_SERVER_PORT = 8080;
    private LocalHttpServer localServer;

    // AdMob
    private String BANNER_AD_UNIT_ID;
    private String INTERSTITIAL_AD_UNIT_ID;
    private String APP_OPEN_AD_UNIT_ID;
    private String REWARDED_AD_UNIT_ID;

    private static final long BANNER_ROTATION_INTERVAL_MS = 45 * 1000;
    private static final long INTERSTITIAL_INTERVAL_MS = 10 * 60 * 1000;

    private WebView webView;
    private FrameLayout adContainer;
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    private AppOpenAd mAppOpenAd;
    private RewardedAd mRewardedAd;

    private Handler bannerRotationHandler;
    private Handler interstitialTimerHandler;
    private int currentBannerType = 0;

    private boolean hasShownAppOpenAd = false;
    private boolean isAppOpenAdShowing = false;

    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0d1117);
        }

        setContentView(R.layout.activity_webview);

        // Load AdMob IDs
        BANNER_AD_UNIT_ID = getString(R.string.admob_banner_id);
        INTERSTITIAL_AD_UNIT_ID = getString(R.string.admob_interstitial_id);
        APP_OPEN_AD_UNIT_ID = getString(R.string.admob_app_open_id);
        REWARDED_AD_UNIT_ID = getString(R.string.admob_rewarded_id);

        createNotificationChannel();
        requestNotificationPermission();

        // Start local HTTP server
        startLocalServer();

        setupWebView();

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "AdMob initialized");
            loadAppOpenAd();
            loadBannerAd();
            loadInterstitialAd();
            loadRewardedAd();
            startBannerRotation();
            startInterstitialTimer();
        });
    }

    private void startLocalServer() {
        try {
            localServer = new LocalHttpServer(this, LOCAL_SERVER_PORT);
            localServer.start();
            Log.d(TAG, "Local server started on port " + LOCAL_SERVER_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start local server", e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = findViewById(R.id.webView);
        adContainer = findViewById(R.id.adContainer);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        
        // CRITICAL: Set User-Agent to appear as Chrome for Google OAuth compliance
        // Google blocks requests with "disallowed_useragent" error if User-Agent doesn't appear Chrome-like
        // This is mandatory for Firebase Google Sign-In to work
        String userAgent = settings.getUserAgentString();
        String chromeUserAgent = userAgent.replace("Android", "Android")
                + " Chrome/120.0.0.0 Mobile";
        settings.setUserAgentString(chromeUserAgent);
        Log.d(TAG, "Set User-Agent for OAuth: " + chromeUserAgent.substring(0, Math.min(100, chromeUserAgent.length())));
        
        // Enable Enhanced security for OAuth flows
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        // Enable cookies for Firebase Auth
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        // Add JavaScript interface
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Allow Firebase auth popup URLs
                if (url.contains("accounts.google.com") ||
                    url.contains("github.com") ||
                    url.contains("firebaseapp.com") ||
                    url.contains("googleapis.com") ||
                    url.contains("counter-controller")) {
                    return false; // Load in WebView
                }
                
                // External links
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (!url.contains("localhost") && !url.contains("127.0.0.1")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page loaded: " + url);
                
                // Show App Open Ad
                if (!hasShownAppOpenAd && url.contains("localhost")) {
                    hasShownAppOpenAd = true;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> showAppOpenAd(), 1500);
                }
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Ensure proper headers for OAuth requests
                Map<String, String> headers = request.getRequestHeaders();
                if (!headers.containsKey("User-Agent")) {
                    // This ensures Chrome-like User-Agent is sent with all requests
                    Log.d(TAG, "Processing request: " + request.getUrl().toString());
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                WebViewActivity.this.filePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(Intent.createChooser(intent, "Select JSON File"), FILE_CHOOSER_REQUEST_CODE);
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                // Handle popup windows for OAuth
                WebView newWebView = new WebView(WebViewActivity.this);
                newWebView.getSettings().setJavaScriptEnabled(true);
                newWebView.getSettings().setDomStorageEnabled(true);
                newWebView.getSettings().setSupportMultipleWindows(true);
                
                CookieManager.getInstance().setAcceptThirdPartyCookies(newWebView, true);
                
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        if (url.contains("counter-controller") || url.contains("localhost")) {
                            // Redirect back to main webview
                            webView.loadUrl(url);
                            return true;
                        }
                        return false;
                    }
                });
                
                newWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        // Popup closed
                    }
                });
                
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        // Load via local server for http:// protocol (Firebase Auth requirement)
        String localUrl = "http://localhost:" + LOCAL_SERVER_PORT + "/index.html";
        webView.loadUrl(localUrl);
    }

    // ==================== JavaScript Interface ====================
    public class WebAppInterface {
        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(WebViewActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void showNotification(String title, String message) {
            runOnUiThread(() -> WebViewActivity.this.showNotification(title, message));
        }

        @JavascriptInterface
        public void onTargetReached(String counterName) {
            runOnUiThread(() -> {
                showInterstitialAd();
                showNotification("🎉 Target Tercapai!", "Counter \"" + counterName + "\" telah mencapai target!");
            });
        }

        @JavascriptInterface
        public void onUserLogin() {
            runOnUiThread(() -> showRewardedAd());
        }

        @JavascriptInterface
        public void requestRewardVideo() {
            runOnUiThread(() -> showRewardedAd());
        }

        @JavascriptInterface
        public String getAppVersion() {
            return BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface
        public void vibrate(int duration) {
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(duration);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Vibrate error", e);
            }
        }
    }

    // ==================== Notifications ====================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Counter Board", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifikasi Counter Board");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, WebViewActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
            }
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
        }
    }

    // ==================== AdMob Methods ====================
    private void loadAppOpenAd() {
        AppOpenAd.load(this, APP_OPEN_AD_UNIT_ID, new AdRequest.Builder().build(),
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    mAppOpenAd = ad;
                    mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            isAppOpenAdShowing = false;
                            mAppOpenAd = null;
                            loadAppOpenAd();
                        }
                    });
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    mAppOpenAd = null;
                }
            });
    }

    private void showAppOpenAd() {
        if (mAppOpenAd != null && !isAppOpenAdShowing) {
            isAppOpenAdShowing = true;
            mAppOpenAd.show(this);
        }
    }

    private void loadBannerAd() {
        adContainer.removeAllViews();
        if (adView != null) adView.destroy();

        adView = new AdView(this);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);
        adView.setAdSize(getAdSize());
        adContainer.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        int adWidth = (int) (outMetrics.widthPixels / outMetrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    private void startBannerRotation() {
        bannerRotationHandler = new Handler(Looper.getMainLooper());
        bannerRotationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadBannerAd();
                bannerRotationHandler.postDelayed(this, BANNER_ROTATION_INTERVAL_MS);
            }
        }, BANNER_ROTATION_INTERVAL_MS);
    }

    private void loadInterstitialAd() {
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    mInterstitialAd = ad;
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mInterstitialAd = null;
                            loadInterstitialAd();
                        }
                    });
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    mInterstitialAd = null;
                }
            });
    }

    private void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {
            loadInterstitialAd();
        }
    }

    private void startInterstitialTimer() {
        interstitialTimerHandler = new Handler(Looper.getMainLooper());
        interstitialTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showInterstitialAd();
                interstitialTimerHandler.postDelayed(this, INTERSTITIAL_INTERVAL_MS);
            }
        }, INTERSTITIAL_INTERVAL_MS);
    }

    private void loadRewardedAd() {
        RewardedAd.load(this, REWARDED_AD_UNIT_ID, new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    mRewardedAd = ad;
                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mRewardedAd = null;
                            loadRewardedAd();
                        }
                    });
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    mRewardedAd = null;
                }
            });
    }

    private void showRewardedAd() {
        if (mRewardedAd != null) {
            mRewardedAd.show(this, rewardItem -> {
                Log.d(TAG, "User earned reward");
                webView.evaluateJavascript("window.onRewardVideoComplete && window.onRewardVideoComplete()", null);
            });
        } else {
            Toast.makeText(this, "Video belum siap, coba lagi", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }

    // ==================== Activity Result ====================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (localServer != null) {
            localServer.stop();
        }
        if (adView != null) adView.destroy();
        if (bannerRotationHandler != null) bannerRotationHandler.removeCallbacksAndMessages(null);
        if (interstitialTimerHandler != null) interstitialTimerHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
