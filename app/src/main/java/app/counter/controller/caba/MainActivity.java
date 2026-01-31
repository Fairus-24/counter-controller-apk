
package app.counter.controller.caba;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationManager;
import android.app.NotificationChannel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
    // All non-native and orphaned code removed for native-only

    // ...existing code for AdMob, drag & drop, toolbar, etc...

    // ==================== NOTIFICATIONS ====================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Counter Board";
            String description = "Notifikasi Counter Board";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    // ...existing code...

    // ==================== APP OPEN AD ====================
    private void loadAppOpenAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        AppOpenAd.load(this, APP_OPEN_AD_UNIT_ID, adRequest,
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

    // ==================== BANNER AD ====================
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

    // ==================== INTERSTITIAL AD ====================
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
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

    // ==================== REWARDED AD ====================
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, REWARDED_AD_UNIT_ID, adRequest,
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

    public void showRewardedAd() {
        if (mRewardedAd != null) {
            mRewardedAd.show(this, rewardItem -> {
                Log.d(TAG, "User earned reward");
                Toast.makeText(this, "Terima kasih!", Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Video belum siap", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        if (bannerRotationHandler != null) bannerRotationHandler.removeCallbacksAndMessages(null);
        if (interstitialTimerHandler != null) interstitialTimerHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
