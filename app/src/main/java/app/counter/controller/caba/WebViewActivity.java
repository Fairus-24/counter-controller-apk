    // Native bridge for JS
    public class WebAppInterface {
        @JavascriptInterface
        public void requestPhoneOtp(String phone) {
            runOnUiThread(() -> {
                Intent intent = new Intent(WebViewActivity.this, LoginActivity.class);
                intent.putExtra("phone_otp_request", true);
                intent.putExtra("phone", phone);
                startActivity(intent);
            });
        }
        @JavascriptInterface
        public void verifyPhoneOtp(String phone, String code) {
            runOnUiThread(() -> {
                Intent intent = new Intent(WebViewActivity.this, LoginActivity.class);
                intent.putExtra("phone_otp_verify", true);
                intent.putExtra("phone", phone);
                intent.putExtra("otp_code", code);
                startActivity(intent);
            });
        }
        @JavascriptInterface
        public void loginWithPhone(String phone) {
            runOnUiThread(() -> {
                Intent intent = new Intent(WebViewActivity.this, LoginActivity.class);
                intent.putExtra("phone_login", true);
                intent.putExtra("phone", phone);
                startActivity(intent);
            });
        }
        @JavascriptInterface
        public void loginWithEmail(String email, String password) {
            runOnUiThread(() -> {
                Intent intent = new Intent(WebViewActivity.this, LoginActivity.class);
                intent.putExtra("email_login", true);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                startActivity(intent);
            });
        }
        @JavascriptInterface
        public void registerWithEmail(String email, String password, String password2) {
            runOnUiThread(() -> {
                Intent intent = new Intent(WebViewActivity.this, LoginActivity.class);
                intent.putExtra("register", true);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                intent.putExtra("password2", password2);
                startActivity(intent);
            });
        }
        @JavascriptInterface
        public void resetPassword(String email) {
            runOnUiThread(() -> {
                Intent intent = new Intent(WebViewActivity.this, LoginActivity.class);
                intent.putExtra("reset_password", true);
                intent.putExtra("email", email);
                startActivity(intent);
            });
        }
        @JavascriptInterface
        public void loginWithGoogle() {
            runOnUiThread(() -> {
                Intent intent = new Intent(WebViewActivity.this, LoginActivity.class);
                intent.putExtra("google_only", true);
                startActivity(intent);
            });
        }
    }

package app.counter.controller.caba;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

/**
 * WebViewActivity - Loads Counter Board HTML for native-only authentication (no Firebase, no OAuth)
 */
public class WebViewActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.setWebViewClient(new WebViewClient());
        // Load your local HTML file or remote URL here
        webView.loadUrl("file:///android_asset/auth.html");
    }

    // ...existing code...

    // ==================== Activity Result ====================

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
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}
