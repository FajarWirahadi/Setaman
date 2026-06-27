package com.example.florist.views;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.ActivityLoginBinding;
import com.example.florist.viewmodels.AuthViewModel;
import com.example.florist.views.homepage.HomepageActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible;
    private ActivityLoginBinding binding;

    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupGoogleSignIn();
        setupObservers();
        setupListeners();

        // Paksa validasi awal agar tombol langsung mati (abu-abu) saat layar pertama kali dibuka
        authViewModel.loginDataChanged(binding.etEmail.getText().toString(), binding.etPassword.getText().toString());
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        String idToken = account.getIdToken();
                        authViewModel.loginWithGoogle(idToken);
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign In Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void setupObservers() {
        // Observasi Validitas Form Login
        authViewModel.getIsLoginFormValid().observe(this, isValid -> {
            binding.btnLogin.setEnabled(isValid);
            if (isValid) {
                binding.btnLogin.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.main_color)));
            } else {
                binding.btnLogin.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray_500)));
            }
        });

        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                Toast.makeText(this, "Selamat datang, " + firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, HomepageActivity.class);
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.btnLogin.setEnabled(false);
                binding.btnLogin.setText("Loading...");
            } else {
                // Kembalikan state tombol sesuai validitas form
                Boolean isValid = authViewModel.getIsLoginFormValid().getValue();
                binding.btnLogin.setEnabled(isValid != null && isValid);
                binding.btnLogin.setText("Masuk");
            }
        });

        authViewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null) {
                if (errorMsg.equals("USER_NOT_FOUND")) {
                    showRegisterSnackbar();
                } else if (errorMsg.equals("PASSWORD_WRONG")) {
                    Toast.makeText(this, "Kata sandi salah, silakan coba lagi.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        authViewModel.getNewGoogleUser().observe(this, firebaseUser -> {
            if (firebaseUser != null ) {
                Intent intent = new Intent (LoginActivity.this, RegisterActivity.class);
                intent.putExtra("EXTRA_EMAIL", firebaseUser.getEmail());
                intent.putExtra("EXTRA_NAME", firebaseUser.getDisplayName());
                intent.putExtra("IS_GOOGLE_SIGNUP", true);
                startActivity(intent);
            }
        });
    }

    private void setupListeners() {
        // Pemantau perubahan teks pada Email dan Password
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                authViewModel.loginDataChanged(
                        binding.etEmail.getText().toString(),
                        binding.etPassword.getText().toString()
                );
            }
        };

        binding.etEmail.addTextChangedListener(afterTextChangedListener);
        binding.etPassword.addTextChangedListener(afterTextChangedListener);

        binding.btnLogin.setOnClickListener(v -> {
            String input = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            authViewModel.login(input, password);
        });

        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterPhoneActivity.class);
            startActivity(intent);
        });

        binding.btnLoginGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        binding.tvShowPassword.setOnClickListener(v -> {
            int cursorPosition = binding.etPassword.getSelectionEnd();

            if (isPasswordVisible) {
                binding.etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.tvShowPassword.setText("Lihat");
            } else {
                binding.etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.tvShowPassword.setText("Tutup");
            }
            binding.etPassword.setSelection(cursorPosition);

            isPasswordVisible = !isPasswordVisible;
        });
    }

    private void showRegisterSnackbar() {
        Snackbar.make(binding.getRoot(), "Akun belum terdaftar.", Snackbar.LENGTH_LONG)
                .setAction("DAFTAR SEKARANG", v -> {
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                })
                .setActionTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}