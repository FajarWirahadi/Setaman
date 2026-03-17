package com.example.florist.views;

import android.animation.ObjectAnimator;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.ActivityRegisterVerificationBinding; // Sesuaikan nama layout binding
import com.example.florist.model.User;
import com.example.florist.viewmodels.AuthViewModel;
import com.example.florist.views.homepage.HomepageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        tempUser = (User) getIntent().getSerializableExtra("EXTRA_USER_DATA");


        updateUI();
        setupOtpInputs();
        setupObservers();
        setupListeners();
        startTimer();

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
                Toast.makeText(this, "Email verifikasi telah dikirim ulang", Toast.LENGTH_SHORT).show();
                startTimer();
            } else  {
            authViewModel.sendOtp(tempUser.getPhoneNumber(), this);
                Toast.makeText(this, "Kode OTP telah dikirim ulang", Toast.LENGTH_SHORT).show();
                startTimer(); // Reset timer
            }
        });

        binding.tvChangeInput.setOnClickListener(v -> {
            if (isEmailFlow) {
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.putExtra("EXTRA_USER_DATA", tempUser);
            } else {
                Intent intent = new Intent(this, RegisterPhoneActivity.class);
                intent.putExtra("EXTRA_USER_DATA", tempUser);
            }
        });
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
                        // Jika diisi, pindah ke kotak berikutnya
                        if (index < editTexts.length - 1) {
                            editTexts[index + 1].requestFocus();
                        } else {
                            // Jika kotak terakhir diisi, otomatis verifikasi (Opsional)
                            verifyOtp();
                        }
                    } else if (s.length() == 0) {
                        // Jika dihapus, pindah ke kotak sebelumnya
                        if (index > 0) {
                            editTexts[index - 1].requestFocus();
                        }
                    }
                }
            });

            // Handle tombol delete/backspace pada keyboard kosong
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

        if (code.length() < 6) {
            // Belum lengkap, jangan submit dulu
            return;
        }

        // PANGGIL VIEWMODEL (Pastikan method verifyAndRegisterUser sudah ada di VM)
        authViewModel.verifyAndRegisterUser(verificationId, code, tempUser);
    }

    private void setupObservers() {
        /// Pantau Sukses (UserLiveData terisi = Proses Selesai)
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                if (isEmailFlow && firebaseUser.isEmailVerified()) {
                    stopAutoCheck();
                Intent intent = new Intent(RegisterVerificationActivity.this, RegisterSuccessActivity.class);
                // Bersihkan stack activity agar user tidak bisa back ke halaman register
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                }

            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            // Bisa tambahkan loading dialog jika mau
        });

        authViewModel.getErrorMessage().observe(this, error -> {
        });

        // Observer khusus Resend OTP (dapat ID baru)
        authViewModel.getOtpSentLiveData().observe(this, newVerificationId -> {
            if (newVerificationId != null) {
                verificationId = newVerificationId; // Update ID tiket
                Toast.makeText(this, "Kode baru terkirim!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startAutoCheck() {
        handler.removeCallbacks(statusChecker);
        statusChecker.run();
    }
    private void stopAutoCheck() {
        handler.removeCallbacks(statusChecker);
    }


    // Fitur Tambahan: Timer Hitung Mundur Resend Code
    private void startTimer() {
        binding.tvChangeInput.setEnabled(false);
        binding.tvChangeInput.setTextColor(getResources().getColor(R.color.gray_500));

        new CountDownTimer(120000, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.countDownTimer.setText(millisUntilFinished / 1000 + " detik");
            }

            public void onFinish() {
                binding.countDownTimer.setText("0 detik");
                binding.tvChangeInput.setEnabled(true);
                binding.tvChangeInput.setTextColor(getResources().getColor(R.color.olive_500));
                binding.tvChangeInput.setText("Kirim Ulang");
            }
        }.start();
    }
}