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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * LoginActivity - Native Firebase Authentication
 * Supports: Google Sign-In, Email/Password, Phone OTP
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private RewardedAd mRewardedAd;

    private MaterialButton btnGoogleSignIn, btnEmailSignIn, btnPhoneSignIn, btnSkip;
    private TextView tvVersion;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle native bridge actions from WebView
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

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
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
            if (getIntent().getBooleanExtra("google_only", false)) {
                Intent result = new Intent();
                result.putExtra("google_id_token", account.getIdToken());
                setResult(Activity.RESULT_OK, result);
                finish();
            } else {
                firebaseAuthWithGoogle(account.getIdToken());
            }
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
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Email reset password terkirim!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Gagal mengirim email reset password.", Toast.LENGTH_SHORT).show();
                }
                finish();
            });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInWithCredential:success");
                    showRewardedAdAndGoToMain();
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    Toast.makeText(this, "Autentikasi gagal", Toast.LENGTH_SHORT).show();
                }
            });
    }

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
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                Intent result = new Intent();
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    result.putExtra("auth_success", true);
                    if (user != null) {
                        result.putExtra("user_email", user.getEmail());
                        result.putExtra("user_uid", user.getUid());
                    }
                    setResult(Activity.RESULT_OK, result);
                    finish();
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    result.putExtra("auth_success", false);
                    result.putExtra("error_message", task.getException() != null ? task.getException().getMessage() : "Login gagal");
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
            });
    }

    private void createAccountWithEmail(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                Intent result = new Intent();
                if (task.isSuccessful()) {
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    result.putExtra("register_success", true);
                    if (user != null) {
                        result.putExtra("user_email", user.getEmail());
                        result.putExtra("user_uid", user.getUid());
                    }
                    setResult(Activity.RESULT_OK, result);
                    finish();
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                    result.putExtra("register_success", false);
                    result.putExtra("error_message", task.getException() != null ? task.getException().getMessage() : "Pendaftaran gagal");
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
            });
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
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    signInWithPhoneCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.w(TAG, "onVerificationFailed", e);
                    Toast.makeText(LoginActivity.this, "Verifikasi gagal: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = vId;
                    resendToken = token;
                    showOTPDialog();
                }
            })
            .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "Mengirim OTP...", Toast.LENGTH_SHORT).show();
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
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                    signInWithPhoneCredential(credential);
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                Intent result = new Intent();
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInWithCredential:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    result.putExtra("phone_login_success", true);
                    if (user != null) {
                        result.putExtra("user_phone", user.getPhoneNumber());
                        result.putExtra("user_uid", user.getUid());
                    }
                    setResult(Activity.RESULT_OK, result);
                    finish();
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    result.putExtra("phone_login_success", false);
                    result.putExtra("error_message", task.getException() != null ? task.getException().getMessage() : "Verifikasi OTP gagal");
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
            });
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
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Auto-retrieval, login immediately
                    signInWithPhoneCredential(credential);
                }
                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Intent result = new Intent();
                    result.putExtra("phone_login_success", false);
                    result.putExtra("error_message", e.getMessage());
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
                @Override
                public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    // Save verificationId for next step
                    getSharedPreferences("otp", MODE_PRIVATE).edit().putString("verificationId", vId).apply();
                    Intent result = new Intent();
                    result.putExtra("otp_requested", true);
                    result.putExtra("phone", phone);
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
            })
            .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "Mengirim OTP...", Toast.LENGTH_SHORT).show();
    }

    // Step 2: Verify OTP
    private void verifyPhoneOtpNative(String phone, String code) {
        String vId = getSharedPreferences("otp", MODE_PRIVATE).getString("verificationId", null);
        if (vId == null) {
            Intent result = new Intent();
            result.putExtra("phone_login_success", false);
            result.putExtra("error_message", "OTP tidak ditemukan. Silakan minta ulang.");
            setResult(Activity.RESULT_OK, result);
            finish();
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(vId, code);
        signInWithPhoneCredential(credential);
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
