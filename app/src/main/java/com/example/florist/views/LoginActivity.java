package com.example.florist.views;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Toast;

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


    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Pastikan ini tidak merah
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
                        // Google Sign In berhasil, ambil akunnya
                        GoogleSignInAccount account = task.getResult(ApiException.class);

                        // Ambil ID Token dari akun tersebut
                        String idToken = account.getIdToken();

                        // Kirim Token ke ViewModel untuk ditukar jadi akun Firebase
                        authViewModel.loginWithGoogle(idToken);

                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign In Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void setupObservers() {
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
                binding.btnLogin.setText("Loading");
            } else {
                binding.btnLogin.setEnabled(true);
                binding.btnLogin.setText("Masuk");
            }
        });

        authViewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null) {
                if (errorMsg.equals("USER_NOT_FOUND")) {
                    // KASUS KHUSUS: Akun belum terdaftar
                    showRegisterSnackbar();
                } else if (errorMsg.equals("PASSWORD_WRONG")) {
                    Toast.makeText(this, "Kata sandi salah, silakan coba lagi.", Toast.LENGTH_SHORT).show();
                } else {
                    // Error umum lainnya
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

        binding.btnLogin.setOnClickListener(v -> {
            String input = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/No HP atau password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
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
            if(isPasswordVisible) {
                String pass = binding.etPassword.toString();
                binding.etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.etPassword.setText(pass);
                binding.etPassword.setSelection(pass.length());
            }else {
                String pass = binding.etPassword.getText().toString();
                binding.etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT);
                binding.etPassword.setText(pass);
                binding.etPassword.setSelection(pass.length());
            }
            isPasswordVisible = !isPasswordVisible;
        });



        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait..");
        progressDialog.setMessage("Loging to your account");
        progressDialog.setCancelable(false);


    }

    private void showRegisterSnackbar() {
        Snackbar.make(binding.getRoot(), "Akun belum terdaftar.", Snackbar.LENGTH_LONG)
                .setAction("DAFTAR SEKARANG", v -> {
                    // Aksi jika tombol dipencet: Pindah ke RegisterActivity
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_green_light)) // Ganti warna teks tombol jika mau
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}