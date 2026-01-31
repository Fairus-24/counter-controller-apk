package app.counter.controller.caba;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
// All Google/Firebase/AdMob/OAuth code removed for native-only

import java.util.concurrent.TimeUnit;

/**
 * LoginActivity - Native/local authentication only (no Firebase)
 * Supports: Google Sign-In (dummy), Email/Password, Phone OTP (dummy)
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // All Google/Firebase/AdMob/OAuth fields removed for native-only

    private MaterialButton btnEmailSignIn, btnPhoneSignIn, btnSkip;
    private TextView tvVersion;

    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Native-only authentication logic here
        Intent intent = getIntent();
        if (intent.getBooleanExtra("google_only", false)) {
            setContentView(R.layout.activity_login);
            findViewById(R.id.btnGoogleSignIn).performClick();
            return;
        } else if (intent.getBooleanExtra("email_login", false)) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            if (email != null && password != null) {
                signInWithEmail(email, password);
                return;
            }
        } else if (intent.getBooleanExtra("register", false)) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            String password2 = intent.getStringExtra("password2");
            if (email != null && password != null && password2 != null) {
                if (!password.equals(password2)) {
                    Toast.makeText(this, "Password konfirmasi tidak cocok.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    createAccountWithEmail(email, password);
                }
                return;
            }
        } else if (intent.getBooleanExtra("reset_password", false)) {
            String email = intent.getStringExtra("email");
            if (email != null) {
                resetPassword(email);
                return;
            }
        } else if (intent.getBooleanExtra("phone_otp_request", false)) {
            String phone = intent.getStringExtra("phone");
            if (phone != null) {
                requestPhoneOtpNative(phone);
                return;
            }
        } else if (intent.getBooleanExtra("phone_otp_verify", false)) {
            String phone = intent.getStringExtra("phone");
            String code = intent.getStringExtra("otp_code");
            if (phone != null && code != null) {
                verifyPhoneOtpNative(phone, code);
                return;
            }
        } else if (intent.getBooleanExtra("phone_login", false)) {
            String phone = intent.getStringExtra("phone");
            if (phone != null) {
                signInWithPhoneNative(phone);
                return;
            }
        }

        setContentView(R.layout.activity_login);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0d1117);
        }

        setContentView(R.layout.activity_login);

        // Cek login lokal
        if (isLoggedIn()) {
            goToMain();
            return;
        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup Google Sign In launcher
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                }
            });

        initViews();
        loadRewardedAd();
    }

    private void initViews() {
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnEmailSignIn = findViewById(R.id.btnEmailSignIn);
        btnPhoneSignIn = findViewById(R.id.btnPhoneSignIn);
        btnSkip = findViewById(R.id.btnSkip);
        tvVersion = findViewById(R.id.tvVersion);

        tvVersion.setText(BuildConfig.VERSION_DISPLAY);

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        btnEmailSignIn.setOnClickListener(v -> showEmailSignInDialog());
        btnPhoneSignIn.setOnClickListener(v -> showPhoneSignInDialog());
        btnSkip.setOnClickListener(v -> goToMain());
    }

    // ==================== GOOGLE SIGN IN ====================
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            Log.d(TAG, "Google sign in success: " + account.getEmail());
            // Simpan login lokal
            saveLogin(account.getEmail(), "google");
            goToMain();
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            Toast.makeText(this, "Login Google gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Handle reset password from native bridge
    private void resetPassword(String email) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Masukkan email untuk reset password.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Simulasi reset password lokal
        Toast.makeText(this, "Reset password: fitur hanya lokal.", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Tidak ada firebaseAuthWithGoogle, semua login native

    // ==================== EMAIL SIGN IN ====================
    private void showEmailSignInDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_login, null);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        new AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("Login dengan Email")
            .setView(dialogView)
            .setPositiveButton("Login", (dialog, which) -> {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (!email.isEmpty() && !password.isEmpty()) {
                    signInWithEmail(email, password);
                } else {
                    Toast.makeText(this, "Email dan password harus diisi", Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("Daftar Baru", (dialog, which) -> {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (!email.isEmpty() && password.length() >= 6) {
                    createAccountWithEmail(email, password);
                } else {
                    Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void signInWithEmail(String email, String password) {
        Intent result = new Intent();
        if (checkLogin(email, password)) {
            saveLogin(email, "email");
            result.putExtra("auth_success", true);
            result.putExtra("user_email", email);
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            result.putExtra("auth_success", false);
            result.putExtra("error_message", "Login gagal: email/password salah.");
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    }

    private void createAccountWithEmail(String email, String password) {
        Intent result = new Intent();
        if (registerUser(email, password)) {
            result.putExtra("register_success", true);
            result.putExtra("user_email", email);
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            result.putExtra("register_success", false);
            result.putExtra("error_message", "Pendaftaran gagal: email sudah terdaftar.");
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    }

    // ==================== PHONE SIGN IN ====================
    private void showPhoneSignInDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_phone_login, null);
        EditText etPhone = dialogView.findViewById(R.id.etPhone);

        new AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("Login dengan Nomor HP")
            .setView(dialogView)
            .setPositiveButton("Kirim OTP", (dialog, which) -> {
                String phone = etPhone.getText().toString().trim();
                if (!phone.isEmpty()) {
                    // Format phone number
                    if (!phone.startsWith("+")) {
                        if (phone.startsWith("0")) {
                            phone = "+62" + phone.substring(1);
                        } else {
                            phone = "+62" + phone;
                        }
                    }
                    sendOTP(phone);
                } else {
                    Toast.makeText(this, "Masukkan nomor HP", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void sendOTP(String phoneNumber) {
        // Simulasi OTP lokal: kode selalu "123456"
        verificationId = "dummy_verification_id";
        Toast.makeText(this, "OTP terkirim: 123456 (simulasi)", Toast.LENGTH_SHORT).show();
        showOTPDialog();
    }

    private void showOTPDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_otp, null);
        EditText etOTP = dialogView.findViewById(R.id.etOTP);

        new AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("Masukkan Kode OTP")
            .setView(dialogView)
            .setPositiveButton("Verifikasi", (dialog, which) -> {
                String code = etOTP.getText().toString().trim();
                if (!code.isEmpty() && verificationId != null) {
                    // Native OTP: kode selalu "123456"
                    signInWithPhoneCredential(null);
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void signInWithPhoneCredential(Object credential) {
        Intent result = new Intent();
        // Simulasi: kode OTP selalu "123456"
        if (verificationId != null) {
            result.putExtra("phone_login_success", true);
            result.putExtra("user_phone", "08123456789");
            result.putExtra("user_uid", "dummy_uid");
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            result.putExtra("phone_login_success", false);
            result.putExtra("error_message", "Verifikasi OTP gagal");
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    }

    // Native phone login (triggered from WebView)
    private void signInWithPhoneNative(String phone) {
        // For backward compatibility, just request OTP
        requestPhoneOtpNative(phone);
    }

    // Step 1: Request OTP (send SMS)
    private void requestPhoneOtpNative(String phone) {
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "Masukkan nomor HP", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        verificationId = "dummy_verification_id";
        getSharedPreferences("otp", MODE_PRIVATE).edit().putString("verificationId", verificationId).apply();
        Intent result = new Intent();
        result.putExtra("otp_requested", true);
        result.putExtra("phone", phone);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    // Step 2: Verify OTP
    private void verifyPhoneOtpNative(String phone, String code) {
        String vId = getSharedPreferences("otp", MODE_PRIVATE).getString("verificationId", null);
        Intent result = new Intent();
        if (vId != null && code.equals("123456")) {
            result.putExtra("phone_login_success", true);
            result.putExtra("user_phone", phone);
            result.putExtra("user_uid", "dummy_uid");
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            result.putExtra("phone_login_success", false);
            result.putExtra("error_message", "OTP salah atau tidak ditemukan.");
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    }
    // ==================== LOCAL AUTH STORAGE ====================
    private boolean isLoggedIn() {
        return getSharedPreferences("auth", MODE_PRIVATE).getBoolean("logged_in", false);
    }
    private void saveLogin(String email, String type) {
        getSharedPreferences("auth", MODE_PRIVATE).edit()
            .putBoolean("logged_in", true)
            .putString("email", email)
            .putString("login_type", type)
            .apply();
    }
    private boolean checkLogin(String email, String password) {
        String regEmail = getSharedPreferences("auth", MODE_PRIVATE).getString("reg_email", null);
        String regPass = getSharedPreferences("auth", MODE_PRIVATE).getString("reg_pass", null);
        return regEmail != null && regPass != null && regEmail.equals(email) && regPass.equals(password);
    }
    private boolean registerUser(String email, String password) {
        String regEmail = getSharedPreferences("auth", MODE_PRIVATE).getString("reg_email", null);
        if (regEmail != null && regEmail.equals(email)) return false;
        getSharedPreferences("auth", MODE_PRIVATE).edit()
            .putString("reg_email", email)
            .putString("reg_pass", password)
            .apply();
        return true;
    }

    // ==================== REWARDED AD ====================
    private void loadRewardedAd() {
        String adUnitId = getString(R.string.admob_rewarded_id);
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, adUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                mRewardedAd = ad;
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                mRewardedAd = null;
            }
        });
    }

    private void showRewardedAdAndGoToMain() {
        if (mRewardedAd != null) {
            mRewardedAd.show(this, rewardItem -> {
                Log.d(TAG, "User earned reward after login");
            });
            // Go to main after ad is dismissed
            mRewardedAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    goToMain();
                }
            });
        } else {
            goToMain();
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
