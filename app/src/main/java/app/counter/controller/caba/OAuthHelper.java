package app.counter.controller.caba;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

/**
 * OAuthHelper - Handles OAuth flows using Chrome Custom Tabs
 * Chrome Custom Tabs provide a more secure environment for authentication
 * and comply with Google's "Use secure browsers" policy
 */
public class OAuthHelper {
    private static final String TAG = "OAuthHelper";
    private static final int DEFAULT_TOOLBAR_COLOR = 0xFF0d1117; // Dark theme color

    /**
     * Open Google OAuth URL in Chrome Custom Tab
     * This ensures Google OAuth works without "disallowed_useragent" error
     */
    public static void openGoogleOAuthInCustomTab(Context context, String authUrl) {
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            
            // Set custom colors for branding
            builder.setToolbarColor(DEFAULT_TOOLBAR_COLOR);
            builder.setSecondaryToolbarColor(DEFAULT_TOOLBAR_COLOR);
            
            // Show title
            builder.setShowTitle(true);
            
            // Enable URL bar
            builder.setUrlBarHidingEnabled(true);
            
            // Build the intent
            CustomTabsIntent customTabsIntent = builder.build();
            
            // Set referrer to indicate this is from your app
            customTabsIntent.intent.putExtra("android.intent.extra.REFERRER", 
                Uri.parse("android-app://" + context.getPackageName()));
            
            // Open the URL in Chrome Custom Tab
            customTabsIntent.launchUrl(context, Uri.parse(authUrl));
            
            Log.d(TAG, "Opened OAuth URL in Chrome Custom Tab: " + authUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error opening OAuth URL in Custom Tab", e);
            // Fallback: you could open in WebView here if needed
        }
    }

    /**
     * Check if Chrome is available on the device
     */
    public static boolean isChromeAvailable(Context context) {
        try {
            context.getPackageManager().getApplicationInfo("com.android.chrome", 0);
            return true;
        } catch (Exception e) {
            // Chrome not installed, fallback to WebView
            return false;
        }
    }
}
