
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import app.counter.controller.caba.adapter.CounterAdapter;
import app.counter.controller.caba.model.AppState;
import app.counter.controller.caba.model.CounterModel;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.PendingIntent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import app.counter.controller.caba.dialog.DialogAddEditCounter;
import app.counter.controller.caba.dialog.DialogStatistics;


public class MainActivity extends AppCompatActivity implements CounterAdapter.CounterListener {
    // --- Fields ---
    private static final int REQ_EXPORT_JSON = 101;
    private static final int REQ_IMPORT_JSON = 102;
    private static final String TAG = "CounterBoard";
    private static final long BANNER_ROTATION_INTERVAL_MS = 45 * 1000;
    private static final long INTERSTITIAL_INTERVAL_MS = 10 * 60 * 1000;
    private static final String CHANNEL_ID = "counter_board_channel";
    private static final int NOTIFICATION_ID = 1;
    private String BANNER_AD_UNIT_ID;
    private String INTERSTITIAL_AD_UNIT_ID;
    private String APP_OPEN_AD_UNIT_ID;
    private String REWARDED_AD_UNIT_ID;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private FrameLayout adContainer;
    private FloatingActionButton fabAdd;
    private MaterialButton btnAddCounter, btnNotes, btnTheme, btnSound, btnFont, btnExport, btnImport, btnSync, btnResetAll;
    private TextView tvAppTitle, tvTotalValue;
    private AppState appState;
    private CounterAdapter adapter;
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    private AppOpenAd mAppOpenAd;
    private RewardedAd mRewardedAd;
    private Handler bannerRotationHandler;
    private Handler interstitialTimerHandler;
    private boolean hasShownAppOpenAd = false;
    private boolean isAppOpenAdShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        
        // Enable offline persistence for Firestore
        try {
            firestore.enableNetwork().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("MainActivity", "enableNetwork failed", task.getException());
                }
            });
            firestore.getFirestoreSettings();
        } catch (Exception e) {
            Log.e("MainActivity", "Error enabling offline persistence", e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0d1117);
        }
        setContentView(R.layout.activity_main_native);

        // Load AdMob IDs from resources
        BANNER_AD_UNIT_ID = getString(R.string.admob_banner_id);
        INTERSTITIAL_AD_UNIT_ID = getString(R.string.admob_interstitial_id);
        APP_OPEN_AD_UNIT_ID = getString(R.string.admob_app_open_id);
        REWARDED_AD_UNIT_ID = getString(R.string.admob_rewarded_id);

        // Create notification channel
        createNotificationChannel();

        // Init state
        appState = new AppState();
        // TODO: Load from storage

        // Init views
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        adContainer = findViewById(R.id.adContainer);
        fabAdd = findViewById(R.id.fabAdd);
        btnAddCounter = findViewById(R.id.btnAddCounter);
        btnNotes = findViewById(R.id.btnNotes);
        btnTheme = findViewById(R.id.btnTheme);
        btnSound = findViewById(R.id.btnSound);
        btnFont = findViewById(R.id.btnFont);
        btnExport = findViewById(R.id.btnExport);
        btnImport = findViewById(R.id.btnImport);
        btnSync = findViewById(R.id.btnSync);
        btnResetAll = findViewById(R.id.btnResetAll);
        tvAppTitle = findViewById(R.id.tvAppTitle);
        tvTotalValue = findViewById(R.id.tvTotalValue);

        // Setup RecyclerView
        adapter = new CounterAdapter(this, appState.getCards(), this);
        int span = getSpanForViewMode(appState.getViewMode());
        GridLayoutManager layoutManager = new GridLayoutManager(this, span);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        // Drag & drop support
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getAdapterPosition();
                int to = target.getAdapterPosition();
                appState.moveCard(from, to);
                adapter.moveItem(from, to);
                saveState();
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {}
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Setup FAB
        fabAdd.setOnClickListener(v -> onAddCounter());
        btnAddCounter.setOnClickListener(v -> onAddCounter());
        // Toolbar actions
        btnNotes.setOnClickListener(v -> {
            appState.setNotesVisible(!appState.isNotesVisible());
            adapter.notifyDataSetChanged();
            saveState();
        });
        btnTheme.setOnClickListener(v -> {
            appState.setTheme(appState.getTheme().equals("dark") ? "light" : "dark");
            // TODO: Apply theme
            saveState();
        });
        btnSound.setOnClickListener(v -> {
            appState.setSoundOn(!appState.isSoundOn());
            // TODO: Update sound icon
            saveState();
        });
        btnFont.setOnClickListener(v -> {
            appState.setFontMode(appState.getFontMode().equals("modern") ? "seven" : "modern");
            adapter.notifyDataSetChanged();
            saveState();
        });
        btnExport.setOnClickListener(v -> {
            // Ekspor AppState ke file JSON
            try {
                String json = new com.google.gson.Gson().toJson(appState);
                File file = new File(getExternalFilesDir(null), "counter_board_export.json");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(json.getBytes());
                }
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Bagikan/Export JSON"));
            } catch (Exception e) {
                Toast.makeText(this, "Export gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        btnImport.setOnClickListener(v -> {
            // Import AppState dari file JSON
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/json");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQ_IMPORT_JSON);
        });
        btnSync.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "Login dulu untuk sync", Toast.LENGTH_SHORT).show();
                return;
            }
            // Upload AppState ke Firestore
            String uid = currentUser.getUid();
            String json = new com.google.gson.Gson().toJson(appState);
            firestore.collection("users").document(uid)
                .set(new java.util.HashMap<String, Object>() {{ put("appState", json); }})
                .addOnSuccessListener(unused -> Toast.makeText(MainActivity.this, "Sync sukses", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Sync gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Jika login, load AppState dari Firestore
        if (currentUser != null) {
            String uid = currentUser.getUid();
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("appState")) {
                        String json = doc.getString("appState");
                        if (json != null) {
                            try {
                                AppState loaded = new com.google.gson.Gson().fromJson(json, AppState.class);
                                if (loaded != null) {
                                    this.appState = loaded;
                                    adapter.setCounters(appState.getCards());
                                    saveState();
                                    updateTotalValue();
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                });
        }
        btnResetAll.setOnClickListener(v -> {
            appState.resetAll();
            adapter.notifyDataSetChanged();
            saveState();
        });

        // Mode tampilan
        View btnViewRow = findViewById(R.id.btnViewRow);
        View btnViewCol = findViewById(R.id.btnViewCol);
        View btnViewGrid = findViewById(R.id.btnViewGrid);
        if (btnViewRow != null) btnViewRow.setOnClickListener(v -> setViewMode("row"));
        if (btnViewCol != null) btnViewCol.setOnClickListener(v -> setViewMode("col"));
        if (btnViewGrid != null) btnViewGrid.setOnClickListener(v -> setViewMode("grid"));

        // AdMob
        MobileAds.initialize(this, status -> {
            loadBannerAd();
            loadAppOpenAd();
            loadInterstitialAd();
            loadRewardedAd();
            startBannerRotation();
            startInterstitialTimer();
        });

        // Show App Open Ad
        if (!hasShownAppOpenAd) {
            hasShownAppOpenAd = true;
            new Handler(Looper.getMainLooper()).postDelayed(this::showAppOpenAd, 1000);
        }

        // Load state
        loadState();
        updateTotalValue();

        // Real-time kolaborasi Firestore
        if (currentUser != null) {
            String uid = currentUser.getUid();
            firestore.collection("users").document(uid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;
                    if (doc != null && doc.exists() && doc.contains("appState")) {
                        String json = doc.getString("appState");
                        if (json != null) {
                            try {
                                AppState loaded = new com.google.gson.Gson().fromJson(json, AppState.class);
                                if (loaded != null && !loaded.equals(this.appState)) {
                                    this.appState = loaded;
                                    adapter.setCounters(appState.getCards());
                                    saveState();
                                    updateTotalValue();
                                    Toast.makeText(this, "Data diperbarui dari cloud", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMPORT_JSON && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    byte[] buf = new byte[4096];
                    int len;
                    StringBuilder sb = new StringBuilder();
                    while ((len = is.read(buf)) > 0) {
                        sb.append(new String(buf, 0, len));
                    }
                    String json = sb.toString();
                    AppState imported = new com.google.gson.Gson().fromJson(json, AppState.class);
                    if (imported != null) {
                        this.appState = imported;
                        adapter.setCounters(appState.getCards());
                        saveState();
                        updateTotalValue();
                        Toast.makeText(this, "Import sukses", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Format file tidak valid", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Import gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Mode tampilan
        // Pastikan ada tombol btnViewRow, btnViewCol, btnViewGrid di layout
        View btnViewRow = findViewById(R.id.btnViewRow);
        View btnViewCol = findViewById(R.id.btnViewCol);
        View btnViewGrid = findViewById(R.id.btnViewGrid);
        if (btnViewRow != null) btnViewRow.setOnClickListener(v -> setViewMode("row"));
        if (btnViewCol != null) btnViewCol.setOnClickListener(v -> setViewMode("col"));
        if (btnViewGrid != null) btnViewGrid.setOnClickListener(v -> setViewMode("grid"));

        // AdMob
        MobileAds.initialize(this, status -> {
            loadBannerAd();
            loadAppOpenAd();
            loadInterstitialAd();
            loadRewardedAd();
            startBannerRotation();
            startInterstitialTimer();
        });

        // Show App Open Ad
        if (!hasShownAppOpenAd) {
            hasShownAppOpenAd = true;
            new Handler(Looper.getMainLooper()).postDelayed(this::showAppOpenAd, 1000);
        }

        // Load state
        loadState();
        updateTotalValue();
    }

    private int getSpanForViewMode(String mode) {
        switch (mode) {
            case "row": return 1;
            case "col": return 2;
            case "grid": return 3;
            default: return 1;
        }
    }

    private void setViewMode(String mode) {
        appState.setViewMode(mode);
        int span = getSpanForViewMode(mode);
        ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanCount(span);
        adapter.notifyDataSetChanged();
        saveState();
    }

    private void saveState() {
        // Simpan appState ke SharedPreferences
        getSharedPreferences("counter_board_prefs", MODE_PRIVATE)
            .edit().putString("app_state_json", new com.google.gson.Gson().toJson(appState)).apply();
    }

    private void loadState() {
        String json = getSharedPreferences("counter_board_prefs", MODE_PRIVATE)
            .getString("app_state_json", null);
        if (json != null) {
            try {
                AppState loaded = new com.google.gson.Gson().fromJson(json, AppState.class);
                if (loaded != null) {
                    this.appState = loaded;
                    adapter.setCounters(appState.getCards());
                }
            } catch (Exception ignored) {}
        }
    }

    private void playSound() {
        if (appState.isSoundOn()) {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80);
            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 80);
            try {
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(40);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateTotalValue() {
        int total = appState.getTotalValue();
        tvTotalValue.setText("Total: " + total);
    }

    @Override
    public void onIncrement(CounterModel counter) {
        boolean reached = counter.increment();
        adapter.notifyDataSetChanged();
        updateTotalValue();
        playSound();
        saveState();
        if (reached && !counter.isTargetNotified()) {
            counter.setTargetNotified(true);
            // Notifikasi target tercapai
            showNotification("🎉 Target Tercapai!", "Counter '" + counter.getLabel() + "' telah mencapai target!");
            showInterstitialAd();
        }
    }

    @Override
    public void onDecrement(CounterModel counter) {
        counter.decrement();
        adapter.notifyDataSetChanged();
        updateTotalValue();
        playSound();
        saveState();
    }

    @Override
    public void onReset(CounterModel counter) {
        counter.reset();
        adapter.notifyDataSetChanged();
        updateTotalValue();
        saveState();
    }

    @Override
    public void onMenu(CounterModel counter, View anchor) {
        // TODO: Show popup menu for edit, delete, duplicate, color, etc.
        Toast.makeText(this, "Menu belum diimplementasi", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDragStart(RecyclerView.ViewHolder viewHolder) {
        // Not needed, handled by ItemTouchHelper
    }

    @Override
    public void onCardClick(CounterModel counter) {
        // Tampilkan dialog edit counter
        DialogAddEditCounter dialog = new DialogAddEditCounter(counter, (updated, isEdit) -> {
            adapter.notifyDataSetChanged();
            saveState();
            updateTotalValue();
        });
        dialog.show(getSupportFragmentManager(), "edit_counter");
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
            }
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void onAddCounter() {
        DialogAddEditCounter dialog = new DialogAddEditCounter(null, (counter, isEdit) -> {
            appState.addCard(counter);
            adapter.notifyItemInserted(appState.getCards().size() - 1);
            saveState();
            updateTotalValue();
        });
        dialog.show(getSupportFragmentManager(), "add_counter");
    }

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
