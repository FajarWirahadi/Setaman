package com.example.florist.views;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.databinding.ActivityRegisterSelectVerificationBinding;
import com.example.florist.model.User;
import com.example.florist.viewmodels.AuthViewModel;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterSelectVerificationActivity extends AppCompatActivity {

    private ActivityRegisterSelectVerificationBinding binding;
    private AuthViewModel authViewModel;
    private User tempUser;
    private boolean isEmailFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterSelectVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        ObjectAnimator.ofInt(binding.progressBar, "progress", 50, 75).setDuration(1000).start();

        setSupportActionBar(binding.toolbar);

        // Mengambil Parcelable
        tempUser = getIntent().getParcelableExtra("EXTRA_USER_DATA");

        if (tempUser != null) {
            binding.tvSMS.setText(authViewModel.formatPhoneNumber(tempUser.getPhoneNumber()));
            binding.tvEmail.setText(tempUser.getEmail());
        }

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        // ViewModel mengontrol status UI (Loading state)
        authViewModel.getIsLoading().observe(this, isLoading -> {
            binding.layoutPhone.setEnabled(!isLoading);
            binding.layoutEmail.setEnabled(!isLoading);
            binding.layoutPhone.setAlpha(isLoading ? 0.5f : 1.0f);
            binding.layoutEmail.setAlpha(isLoading ? 0.5f : 1.0f);
        });

        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null && isEmailFlow) {
                authViewModel.sendVerificationEmail();
                isEmailFlow = false;
            }
        });

        authViewModel.getErrorMessage().observe(this, message -> {
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        // Navigasi dikendalikan murni oleh observer
        authViewModel.getOtpSentLiveData().observe(this, verificationId -> {
            navigateToVerification(verificationId, false);
        });

        authViewModel.getEmailVerificationMsg().observe(this, verificationId -> {
            navigateToVerification(verificationId, true);
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.layoutPhone.setOnClickListener(v -> {
            if (tempUser != null && tempUser.getPhoneNumber() != null) {
                isEmailFlow = false;
                initiatePhoneAuth(authViewModel.formatPhoneNumber(tempUser.getPhoneNumber()));
            } else {
                Toast.makeText(this, "Data nomor HP hilang", Toast.LENGTH_SHORT).show();
            }
        });

        binding.layoutEmail.setOnClickListener(v -> {
            if (tempUser != null && tempUser.getEmail() != null && tempUser.getPassword() != null) {
                isEmailFlow = true;
                authViewModel.register(tempUser);
            } else {
                Toast.makeText(this, "Data email hilang", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Activity HANYA bertugas sebagai jembatan Firebase API, bukan pengolah logika
    private void initiatePhoneAuth(String formattedPhone) {
        authViewModel.setLoading(true); // Lempar tugas loading ke ViewModel
        Toast.makeText(this, "Meminta kode OTP...", Toast.LENGTH_SHORT).show();

        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Jangan proses manual, lempar ke ViewModel
                        authViewModel.handleVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        // Lempar error ke ViewModel untuk menghentikan loading dan set pesan error
                        authViewModel.handleVerificationFailed(e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        // Lempar token sukses ke ViewModel
                        authViewModel.handleCodeSent(verificationId);
                    }
                };

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this) // Ini alasan logisnya tetap di Activity
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void navigateToVerification(String verificationId, boolean isEmail) {
        Intent intent = new Intent(this, RegisterVerificationActivity.class);
        intent.putExtra("EXTRA_VERIFICATION_ID", verificationId);
        intent.putExtra("EXTRA_USER_DATA", tempUser);
        intent.putExtra("EXTRA_IS_EMAIL_FLOW", isEmail);
        startActivity(intent);
    }
}