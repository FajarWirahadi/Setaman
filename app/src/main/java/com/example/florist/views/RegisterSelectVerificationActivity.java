package com.example.florist.views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.florist.R;
import com.example.florist.databinding.ActivityRegisterSelectVerificationBinding;
import com.example.florist.model.User;
import com.example.florist.viewmodels.AuthViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
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

        ObjectAnimator.ofInt(binding.progressBar, "progress",50, 75)
                .setDuration(1000)
                .start();

        setSupportActionBar(binding.toolbar);

        tempUser = (User) getIntent().getSerializableExtra("EXTRA_USER_DATA");

        binding.tvSMS.setText(tempUser.getPhoneNumber());
        binding.tvEmail.setText(tempUser.getEmail());

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        authViewModel.getIsLoading().observe(this, isLoading -> {
            // Agar tidak double click
            binding.layoutPhone.setEnabled(!isLoading);
            binding.layoutEmail.setEnabled(!isLoading);
            binding.layoutPhone.setAlpha(isLoading? 0.5f : 1.0f);
        });

        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null && isEmailFlow) {
                authViewModel.sendVerificationEmail();

                isEmailFlow = false;
            }
        });

        authViewModel.getErrorMessage().observe(this, message -> {
            if(message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        authViewModel.getOtpSentLiveData().observe(this, verificationId -> {
            Intent intent = new Intent(RegisterSelectVerificationActivity.this, RegisterVerificationActivity.class);

            intent.putExtra("EXTRA_VERIFICATION_ID", verificationId);
            intent.putExtra("EXTRA_USER_DATA", tempUser);
            intent.putExtra("EXTRA_IS_EMAIL_FLOW", false);
            startActivity(intent);
        });

        authViewModel.getEmailVerificationMsg().observe(this, verificationId -> {
            Intent intent = new Intent(RegisterSelectVerificationActivity.this, RegisterVerificationActivity.class);
            intent.putExtra("EXTRA_VERIFICATION_ID", verificationId);
            intent.putExtra("EXTRA_USER_DATA", tempUser);
            intent.putExtra("EXTRA_IS_EMAIL_FLOW", true);
            startActivity(intent);
        });
    }

    private void setupListeners() {

        binding.btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
        binding.layoutPhone.setOnClickListener(v -> {
            if (tempUser.getPhoneNumber() != null) {
                isEmailFlow = false;
                authViewModel.sendOtp(tempUser.getPhoneNumber(), this);
            } else {
                Toast.makeText(this, "Data nomor HP hilang, mohon isi ulang", Toast.LENGTH_SHORT).show();
            }
        });

        binding.layoutEmail.setOnClickListener(v -> {
            if (tempUser.getEmail() != null && tempUser.getPassword() != null) {
                isEmailFlow = true;
                authViewModel.register(tempUser);
            } else {
                Toast.makeText(this, "Data email hilang, mohon isi ulang", Toast.LENGTH_SHORT).show();
            }
        });
    }

}