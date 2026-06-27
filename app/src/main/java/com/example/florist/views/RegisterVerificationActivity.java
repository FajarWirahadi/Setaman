package com.example.florist.views;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.ActivityRegisterVerificationBinding;
import com.example.florist.model.User;
import com.example.florist.viewmodels.AuthViewModel;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterVerificationActivity extends AppCompatActivity {

    private ActivityRegisterVerificationBinding binding;
    private AuthViewModel authViewModel;

    private String verificationId;
    private User tempUser;
    private boolean isEmailFlow;
    private Handler handler;
    private static final int CHECK_INTERVAL = 2000;
    private Runnable statusChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        ObjectAnimator.ofInt(binding.progressBar,"progress",75, 90)
                .setDuration(1000)
                .start();

        Intent intent = getIntent();
        verificationId = intent.getStringExtra("EXTRA_VERIFICATION_ID");
        isEmailFlow = intent.getBooleanExtra("EXTRA_IS_EMAIL_FLOW", false);

        // PERBAIKAN FATAL: Menangkap Parcelable dengan aman sesuai versi Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tempUser = intent.getParcelableExtra("EXTRA_USER_DATA", User.class);
        } else {
            tempUser = intent.getParcelableExtra("EXTRA_USER_DATA");
        }

        if (tempUser == null) {
            Toast.makeText(this, "Data pengguna hilang!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateUI();
        setupOtpInputs();
        setupObservers();
        setupListeners();
        startTimer();

        // Polling untuk mengecek status verifikasi email (Ini diizinkan karena Firebase tidak memberikan push notification otomatis untuk email)
        handler = new Handler(Looper.getMainLooper());
        statusChecker = new Runnable() {
            @Override
            public void run() {
                authViewModel.refreshUserStatus();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isEmailFlow) {
            authViewModel.refreshUserStatus();
            startAutoCheck();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoCheck();
    }

    private void updateUI() {
        if (isEmailFlow) {
            binding.tvInput.setText(tempUser.getEmail());
            binding.verificationMethod.setText("Verifikasi melalui Email");
            binding.hintVerificationMethod.setText("Silahkan klik link yang terima melalui");
            binding.method.setText("Email ");
            binding.layoutOtp.setVisibility(View.GONE);
            binding.tvInputSalah.setText("Email salah ?");
        } else {
            binding.tvInput.setText(tempUser.getPhoneNumber());
        }
    }

    private void setupListeners() {
        binding.tvResendVerification.setOnClickListener(v -> {
            if (isEmailFlow) {
                authViewModel.sendVerificationEmail();
                startTimer();
            } else  {
                resendOtpDirectly(tempUser.getPhoneNumber());
            }
        });

        binding.tvChangeInput.setOnClickListener(v -> {
            Intent intent = new Intent(this, isEmailFlow ? RegisterActivity.class : RegisterPhoneActivity.class);
            intent.putExtra("EXTRA_USER_DATA", tempUser);
            startActivity(intent);
            finish();
        });
    }

    // PERBAIKAN MVVM: Activity memanggil Firebase, tapi Callback dilempar ke ViewModel
    private void resendOtpDirectly(String phoneNumber) {
        String formattedPhone = authViewModel.formatPhoneNumber(phoneNumber);
        authViewModel.setLoading(true);

        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        authViewModel.handleVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        authViewModel.handleVerificationFailed(e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String newVerificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = newVerificationId; // Simpan ID baru
                        authViewModel.handleCodeSent(newVerificationId);
                        Toast.makeText(RegisterVerificationActivity.this, "Kode OTP baru berhasil dikirim!", Toast.LENGTH_SHORT).show();
                        startTimer();
                    }
                };

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void setupOtpInputs() {
        EditText[] editTexts = {
                binding.editTxt1, binding.editTxt2, binding.editTxt3,
                binding.editTxt4, binding.editTxt5, binding.editTxt6
        };

        for (int i = 0; i < editTexts.length; i++) {
            final int index = i;
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        if (index < editTexts.length - 1) {
                            editTexts[index + 1].requestFocus();
                        } else {
                            verifyOtp();
                        }
                    } else if (s.length() == 0) {
                        if (index > 0) {
                            editTexts[index - 1].requestFocus();
                        }
                    }
                }
            });

            editTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (editTexts[index].getText().toString().isEmpty() && index > 0) {
                        editTexts[index - 1].requestFocus();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void verifyOtp() {
        String code = binding.editTxt1.getText().toString() +
                binding.editTxt2.getText().toString() +
                binding.editTxt3.getText().toString() +
                binding.editTxt4.getText().toString() +
                binding.editTxt5.getText().toString() +
                binding.editTxt6.getText().toString();

        if (code.length() < 6) return; // Cegah verifikasi prematur

        // Eksekusi via ViewModel
        authViewModel.verifyAndRegisterUser(verificationId, code, tempUser);
    }

    private void setupObservers() {
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                if (isEmailFlow) {
                    if (firebaseUser.isEmailVerified()) {
                        stopAutoCheck();
                        navigateToSuccess();
                    }
                } else {
                    // Jika OTP (Telepon), langsung navigasi
                    navigateToSuccess();
                }
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            binding.layoutOtp.setAlpha(isLoading ? 0.5f : 1.0f);
            binding.tvResendVerification.setEnabled(!isLoading);
        });

        authViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                // Opsional: Kosongkan OTP jika gagal
                binding.editTxt1.setText(""); binding.editTxt2.setText("");
                binding.editTxt3.setText(""); binding.editTxt4.setText("");
                binding.editTxt5.setText(""); binding.editTxt6.setText("");
                binding.editTxt1.requestFocus();
            }
        });

        authViewModel.getEmailVerificationMsg().observe(this, msg -> {
            if (msg != null && isEmailFlow) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSuccess() {
        Intent intent = new Intent(RegisterVerificationActivity.this, RegisterSuccessActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startAutoCheck() {
        handler.removeCallbacks(statusChecker);
        statusChecker.run();
    }

    private void stopAutoCheck() {
        handler.removeCallbacks(statusChecker);
    }

    private void startTimer() {
        binding.tvResendVerification.setEnabled(false);
        binding.tvResendVerification.setTextColor(getResources().getColor(R.color.gray_500));

        new CountDownTimer(120000, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.countDownTimer.setText(millisUntilFinished / 1000 + " detik");
            }

            public void onFinish() {
                binding.countDownTimer.setText("0 detik");
                binding.tvResendVerification.setEnabled(true);
                binding.tvResendVerification.setTextColor(getResources().getColor(R.color.olive_500));
            }
        }.start();
    }
}