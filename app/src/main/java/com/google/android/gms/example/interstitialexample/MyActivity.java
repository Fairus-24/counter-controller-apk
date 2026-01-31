/*
 * Counter Board - Digital Counter App with AdMob Integration
 * Copyright (C) 2026
 */
package com.google.android.gms.example.interstitialexample;

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
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import app.counter.controller.caba.BuildConfig;
import app.counter.controller.caba.R;

import androidx.browser.customtabs.CustomTabsIntent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Counter Board Activity - WebView based counter app with AdMob integration
 * Features: App Open Ads, Interstitial on target/10min, Reward Video after login
 */
public class MyActivity extends AppCompatActivity {

    private static final String TAG = "CounterBoard";
    
    // AdMob Ad Unit IDs - loaded from res/values/admob_ids.xml
    // Untuk mengganti ID: buka file app/src/main/res/values/admob_ids.xml
    private String BANNER_AD_UNIT_ID;
    private String BANNER_AD_UNIT_ID_2;
    private String INTERSTITIAL_AD_UNIT_ID;
    private String APP_OPEN_AD_UNIT_ID;
    private String REWARDED_AD_UNIT_ID;

    // Banner ad rotation settings
    private static final long BANNER_ROTATION_INTERVAL_MS = 45 * 1000; // 45 seconds
    private Handler bannerRotationHandler;
    private Runnable bannerRotationRunnable;
    private int currentBannerType = 0;
    private static final int BANNER_TYPE_ADAPTIVE = 0;
    private static final int BANNER_TYPE_SMART = 1;
    private static final int BANNER_TYPE_MEDIUM_RECT = 2;
    private static final int BANNER_TYPE_COUNT = 3;

    // Notification
    private static final String CHANNEL_ID = "counter_board_channel";
    private static final int NOTIFICATION_PERMISSION_CODE = 1002;
    private static final int NOTIFICATION_ID = 1;

    private WebView webView;
    private FrameLayout adContainer;
    private AdView adView;
    private AdView adView2; // Second banner for rotation
    private InterstitialAd mInterstitialAd;
    private AppOpenAd mAppOpenAd;
    private RewardedAd mRewardedAd;
    
    private Handler timerHandler;
    private Runnable interstitialTimerRunnable;
    private static final long INTERSTITIAL_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
    
    private boolean isAppOpenAdShowing = false;
    private boolean hasShownAppOpenAd = false;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load AdMob Unit IDs from resources (edit in res/values/admob_ids.xml)
        BANNER_AD_UNIT_ID = getString(R.string.admob_banner_id);
        BANNER_AD_UNIT_ID_2 = getString(R.string.admob_banner_medium_rect_id);
        INTERSTITIAL_AD_UNIT_ID = getString(R.string.admob_interstitial_id);
        APP_OPEN_AD_UNIT_ID = getString(R.string.admob_app_open_id);
        REWARDED_AD_UNIT_ID = getString(R.string.admob_rewarded_id);
        
        // Make status bar match app theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0d1117);
        }
        
        setContentView(R.layout.activity_my);

        // Create notification channel
        createNotificationChannel();
        
        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "AdMob SDK initialized");
            loadAppOpenAd();
            loadBannerAd();
            loadInterstitialAd();
            loadRewardedAd();
        });

        setupWebView();
        setupInterstitialTimer();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Counter Board";
            String description = "Notifikasi Counter Board";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Log.d(TAG, "Notification permission denied");
            }
        }
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, MyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    // Buka URL di Chrome Custom Tabs (untuk OAuth - menghindari blokir Google)
    private void openInCustomTab(String url) {
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            builder.setUrlBarHidingEnabled(true);
            
            // Set toolbar color sesuai tema app
            builder.setToolbarColor(0xFF0d1117);
            
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            // Fallback ke browser biasa jika Chrome Custom Tabs tidak tersedia
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = findViewById(R.id.webView);
        adContainer = findViewById(R.id.adContainer);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        // Enable modern web features
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Add JavaScript interface for Android bridge
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // OAuth URLs - gunakan Chrome Custom Tabs (aman dari blokir Google)
                if (url.contains("accounts.google.com") ||
                    url.contains("github.com/login") ||
                    url.contains("github.com/sessions") ||
                    url.contains("github.com/oauth")) {
                    // Buka di Chrome Custom Tabs untuk OAuth
                    openInCustomTab(url);
                    return true;
                }
                
                // Firebase auth redirect URLs - load dalam WebView
                if (url.contains("firebaseapp.com") ||
                    url.contains("firebase") ||
                    url.contains("googleapis.com") ||
                    url.contains("gstatic.com") ||
                    url.contains("counter-controller") ||
                    url.startsWith("file://")) {
                    view.loadUrl(url);
                    return true;
                }
                
                // Open other external links in browser
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page loaded: " + url);
                
                // Show App Open Ad after page loads (first time only)
                if (!hasShownAppOpenAd) {
                    hasShownAppOpenAd = true;
                    showAppOpenAd();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                MyActivity.this.filePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(Intent.createChooser(intent, "Select JSON File"), FILE_CHOOSER_REQUEST_CODE);
                return true;
            }
        });

        // Load the Counter Board HTML from assets
        webView.loadUrl("file:///android_asset/index.html");
    }

    // ==================== APP OPEN AD ====================
    private void loadAppOpenAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        AppOpenAd.load(this, APP_OPEN_AD_UNIT_ID, adRequest,
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                    mAppOpenAd = appOpenAd;
                    Log.d(TAG, "App Open Ad loaded");
                    
                    mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            isAppOpenAdShowing = false;
                            mAppOpenAd = null;
                            loadAppOpenAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            isAppOpenAdShowing = false;
                            mAppOpenAd = null;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isAppOpenAdShowing = true;
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: " + loadAdError.getMessage());
                    mAppOpenAd = null;
                }
            });
    }

    private void showAppOpenAd() {
        if (mAppOpenAd != null && !isAppOpenAdShowing) {
            mAppOpenAd.show(this);
        }
    }

    // ==================== BANNER AD ====================

    private void loadBannerAd() {
        loadBannerAdWithType(BANNER_TYPE_ADAPTIVE);
        setupBannerRotation();
    }
    
    private void loadBannerAdWithType(int bannerType) {
        // Remove existing ad views
        adContainer.removeAllViews();
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        if (adView2 != null) {
            adView2.destroy();
            adView2 = null;
        }
        
        adView = new AdView(this);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);
        
        // Get banner size based on type
        AdSize adSize;
        switch (bannerType) {
            case BANNER_TYPE_SMART:
                adSize = AdSize.SMART_BANNER;
                Log.d(TAG, "Loading Smart Banner");
                break;
            case BANNER_TYPE_MEDIUM_RECT:
                adSize = AdSize.MEDIUM_RECTANGLE;
                Log.d(TAG, "Loading Medium Rectangle Banner");
                break;
            case BANNER_TYPE_ADAPTIVE:
            default:
                adSize = getAdSize();
                Log.d(TAG, "Loading Adaptive Banner");
                break;
        }
        
        adView.setAdSize(adSize);
        
        // Set background color to match app theme
        adContainer.setBackgroundColor(0xFF0d1117);
        adContainer.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        
        currentBannerType = bannerType;
    }
    
    private void setupBannerRotation() {
        bannerRotationHandler = new Handler(Looper.getMainLooper());
        bannerRotationRunnable = new Runnable() {
            @Override
            public void run() {
                // Rotate to next banner type
                int nextType = (currentBannerType + 1) % BANNER_TYPE_COUNT;
                Log.d(TAG, "Rotating banner from type " + currentBannerType + " to " + nextType);
                loadBannerAdWithType(nextType);
                
                // Schedule next rotation
                bannerRotationHandler.postDelayed(this, BANNER_ROTATION_INTERVAL_MS);
            }
        };
        
        // Start banner rotation
        bannerRotationHandler.postDelayed(bannerRotationRunnable, BANNER_ROTATION_INTERVAL_MS);
    }

    private AdSize getAdSize() {
        // Determine the screen width to use for the ad width
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Return adaptive banner size
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.d(TAG, "Interstitial ad loaded");
                    
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad dismissed");
                            mInterstitialAd = null;
                            loadInterstitialAd(); // Preload next ad
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                            mInterstitialAd = null;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad shown");
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                    mInterstitialAd = null;
                }
            });
    }

    private void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {
            Log.d(TAG, "Interstitial ad not ready yet");
            loadInterstitialAd();
        }
    }

    // ==================== REWARDED AD ====================
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, REWARDED_AD_UNIT_ID, adRequest,
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                    mRewardedAd = rewardedAd;
                    Log.d(TAG, "Rewarded ad loaded");
                    
                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mRewardedAd = null;
                            loadRewardedAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            mRewardedAd = null;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            Log.d(TAG, "Rewarded ad shown");
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                    mRewardedAd = null;
                }
            });
    }

    private void showRewardedAd() {
        if (mRewardedAd != null) {
            mRewardedAd.show(this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    Log.d(TAG, "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                    // Notify WebView that user completed reward video
                    runOnUiThread(() -> {
                        webView.evaluateJavascript("window.onRewardVideoComplete && window.onRewardVideoComplete()", null);
                    });
                }
            });
        } else {
            Toast.makeText(this, "Video belum siap, coba lagi", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }

    // ==================== 10-MINUTE INTERSTITIAL TIMER ====================
    private void setupInterstitialTimer() {
        timerHandler = new Handler(Looper.getMainLooper());
        interstitialTimerRunnable = new Runnable() {
            @Override
            public void run() {
                showInterstitialAd();
                timerHandler.postDelayed(this, INTERSTITIAL_INTERVAL_MS);
            }
        };
        // Start the timer
        timerHandler.postDelayed(interstitialTimerRunnable, INTERSTITIAL_INTERVAL_MS);
    }

    // Track user actions and show interstitial periodically
    public void trackAction() {
        // Removed - now using timer and target-based interstitials
    }

    // JavaScript Interface for communication between WebView and Android
    public class WebAppInterface {
        
        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(MyActivity.this, message, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void showNotification(String title, String message) {
            runOnUiThread(() -> MyActivity.this.showNotification(title, message));
        }

        @JavascriptInterface
        public void onTargetReached(String counterName) {
            // Show interstitial when target is reached
            runOnUiThread(() -> {
                showInterstitialAd();
                // Also show notification
                MyActivity.this.showNotification("🎉 Target Tercapai!", 
                    "Counter \"" + counterName + "\" telah mencapai target!");
            });
        }

        @JavascriptInterface
        public void onUserLogin() {
            // Show rewarded video after user logs in
            runOnUiThread(() -> showRewardedAd());
        }

        @JavascriptInterface
        public void requestRewardVideo() {
            runOnUiThread(() -> showRewardedAd());
        }

        @JavascriptInterface
        public void trackCounterAction() {
            // Keep for compatibility, but interstitials are now timer/target based
        }

        @JavascriptInterface
        public void importFile() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(Intent.createChooser(intent, "Select JSON File"), FILE_CHOOSER_REQUEST_CODE);
            });
        }

        @JavascriptInterface
        public void shareText(String text) {
            runOnUiThread(() -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            });
        }

        @JavascriptInterface
        public boolean hasNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(MyActivity.this, 
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            }
            return true;
        }

        @JavascriptInterface
        public void requestNotificationPermission() {
            runOnUiThread(() -> MyActivity.this.requestNotificationPermission());
        }

        @JavascriptInterface
        public String getAppVersion() {
            // Return version in format "versi X.XX.XXXX"
            return BuildConfig.VERSION_DISPLAY;
        }
    }

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
                        
                        // Read and pass JSON to WebView
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(Uri.parse(dataString));
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            reader.close();
                            
                            String json = sb.toString().replace("'", "\\'").replace("\n", "\\n");
                            webView.evaluateJavascript("window.importDataFromAndroid('" + json + "')", null);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading file: " + e.getMessage());
                            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
                        }
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
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        if (adView2 != null) {
            adView2.resume();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        if (adView2 != null) {
            adView2.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Stop interstitial timer
        if (timerHandler != null && interstitialTimerRunnable != null) {
            timerHandler.removeCallbacks(interstitialTimerRunnable);
        }
        // Stop banner rotation
        if (bannerRotationHandler != null && bannerRotationRunnable != null) {
            bannerRotationHandler.removeCallbacks(bannerRotationRunnable);
        }
        // Destroy ad views
        if (adView != null) {
            adView.destroy();
        }
        if (adView2 != null) {
            adView2.destroy();
        }
        // Destroy webview
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
