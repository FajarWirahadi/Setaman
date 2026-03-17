package com.example.florist.views;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.ActivityRegisterBinding;
import com.example.florist.model.AuthRepository;
import com.example.florist.model.User;
import com.example.florist.viewmodels.AuthViewModel;
import com.example.florist.views.homepage.HomepageActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private String phoneNumber;
    private AuthViewModel authViewModel;

    // Variabel untuk melacak status Show/Hide password
    private boolean isPassword1Visible = false;
    private boolean isPassword2Visible = false;
    private boolean isGoogleSignup = false;

    // Pattern Regex untuk validasi
    // ^                 : awal string
    // (?=.*[0-9])       : minimal ada 1 angka
    // (?=.*[A-Z])       : minimal ada 1 huruf besar
    // .{8,}             : minimal 8 karakter apapun
    // $                 : akhir string
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[A-Z]).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setSupportActionBar(binding.toolbar);

        // Matikan judul bawaan Android agar TextView custom kamu (toolbar_title) yang terlihat dan ter-center
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Setup Progress Bar
        ObjectAnimator.ofInt(binding.progressBar, "progress", 25, 50)
                .setDuration(1000) // Durasi animasi 1 detik
                .start();

        // Ambil data dari halaman sebelumnya
        phoneNumber = getIntent().getStringExtra("EXTRA_PHONE");
        String emailGoogle = getIntent().getStringExtra("EXTRA_EMAIL");
        String nameGoogle = getIntent().getStringExtra("EXTRA_NAME");
        isGoogleSignup = getIntent().getBooleanExtra("IS_GOOGLE_SIGNUP", false);

        if (emailGoogle != null) {
            binding.editTextEmail.setText(emailGoogle);
            binding.editTextEmail.setEnabled(false);
        }
        if (nameGoogle != null) {
            binding.editTextUserName.setText(nameGoogle);
            binding.editTextUserName.requestFocus();

        }

        // 1. Setup Fitur Password (Show/Hide & Validasi)
        setupPasswordFeatures();

        // 2. Setup Tombol Daftar
        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.editTextUserName.getText().toString().trim();
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPasswordRegister.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua data", Toast.LENGTH_SHORT).show();
                return;
            }


            authViewModel.checkEmailAvailability(email, new AuthRepository.CheckEmailAvailabitiy() {
                @Override
                public void onResult(boolean isAvailable) {
                    if (isAvailable) {
                        submitForm(email, username, password);
                    } else {
                        binding.editTextEmail.setError("Email sudah terdaftar!");
                        Snackbar.make(binding.getRoot(), "Email sudah digunakan pengguna lain", Snackbar.LENGTH_LONG).show();
                        binding.editTextEmail.requestFocus();
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(RegisterActivity.this, "Gagal cek email " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // Default tombol dimatikan dulu
        updateButtonState(false);
    }

    private void setupPasswordFeatures() {
        // --- A. FITUR SHOW/HIDE PASSWORD ---

        // Listener untuk Password 1
        binding.textViewShowPassword.setOnClickListener(v -> {
            isPassword1Visible = togglePasswordVisibility(binding.editTextPasswordRegister, binding.textViewShowPassword, isPassword1Visible);
        });

        // Listener untuk Password 2 (Confirm)
        binding.textViewShowPassword2.setOnClickListener(v -> {
            isPassword2Visible = togglePasswordVisibility(binding.editTextPasswordRegister2, binding.textViewShowPassword2, isPassword2Visible);
        });

        // --- B. FITUR VALIDASI INPUT (REAL-TIME) ---

        // TextWatcher untuk Password Utama
        binding.editTextPasswordRegister.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
        });

        // TextWatcher untuk Konfirmasi Password
        binding.editTextPasswordRegister2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs(); // Cek validasi setiap mengetik
            }
        });

        // TextWatcher untuk Username & Email (Optional: agar tombol nyala jika semua terisi)
        binding.editTextUserName.addTextChangedListener(genericTextWatcher);
        binding.editTextEmail.addTextChangedListener(genericTextWatcher);
    }

    // Fungsi Helper: Mengubah icon mata/text dan transformasi text
    private boolean togglePasswordVisibility(EditText editText, TextView toggleView, boolean isVisible) {
        if (isVisible) {
            // Sembunyikan Password
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            toggleView.setText("Lihat"); // Atau ganti icon mata dicoret
            // toggleView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
        } else {
            // Tampilkan Password
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            toggleView.setText("Tutup"); // Atau ganti icon mata terbuka
            // toggleView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_on, 0);
        }
        // Kembalikan kursor ke akhir teks
        editText.setSelection(editText.getText().length());
        return !isVisible;
    }

    // Fungsi Helper: Validasi Semua Input
    private void validateInputs() {
        String password = binding.editTextPasswordRegister2.getText().toString();
        String confirmPassword = binding.editTextPasswordRegister.getText().toString();
        String username = binding.editTextUserName.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.setError("Format email tidak valid");
            return;
        }

        // 1. Cek Kriteria Password Satu per Satu
        boolean hasMinLength = password.length() >= 8;
        boolean hasUppercase = password.matches(".*[A-Z].*"); // Regex cek huruf besar
        boolean hasNumber = password.matches(".*[0-9].*");    // Regex cek angka

        // 3. Validasi Total untuk Tombol Register
        boolean isPasswordValid = hasMinLength && hasUppercase && hasNumber;
        boolean isConfirmValid = !confirmPassword.isEmpty() && confirmPassword.equals(password);

        binding.editTextPasswordRegister2.setBackground(AppCompatResources.getDrawable(this, R.drawable.rounded_success_edittext));
        updateCriteriaStyle(hasMinLength, binding.minimumCharPasswordCheck, binding.minimumCharPassword);
        updateCriteriaStyle(hasUppercase, binding.uppercaseCharPasswordCheck, binding.uppercaseCharPassword);
        updateCriteriaStyle(hasNumber, binding.minimumNumberPasswordCheck, binding.minimumNumberPassword);

        // Update border EditText Password
        if (isPasswordValid) {

            // 2. Update Tampilan UI (Centang & Warna Teks)

        } else {
            // Balik ke normal atau error jika belum valid
            binding.editTextPasswordRegister2.setBackground(AppCompatResources.getDrawable(this, R.drawable.rounded_normal_edittext));
        }

        // Update border EditText Confirm Password
        if (isConfirmValid) {
            binding.editTextPasswordRegister.setBackground(AppCompatResources.getDrawable(this, R.drawable.rounded_success_edittext));
        } else {
            binding.editTextPasswordRegister.setBackground(AppCompatResources.getDrawable(this, R.drawable.rounded_normal_edittext));
        }

        // 4. Aktifkan Tombol jika SEMUA valid
        if (isPasswordValid && isConfirmValid && !username.isEmpty() && !email.isEmpty()) {
            updateButtonState(true);
        } else {
            updateButtonState(false);
        }
    }

    // --- METHOD BARU: Untuk Mengubah Warna & Icon Indikator ---
    private void updateCriteriaStyle(boolean isValid, ImageView iconView, TextView textView) {
        if (isValid) {
            // Jika Valid: Icon Hijau (active_check) & Teks Hijau
            iconView.setImageResource(R.drawable.success_check); // Pastikan drawable ini ada
            textView.setTextColor(getResources().getColor(R.color.olive_500)); // Sesuaikan warna hijaumu
        } else {
            // Jika Tidak Valid: Icon Abu (normal_check) & Teks Abu
            iconView.setImageResource(R.drawable.normal_check);
            textView.setTextColor(getResources().getColor(R.color.gray_500));
        }
    }

    private void updateButtonState(boolean isEnabled) {
        binding.btnRegister.setEnabled(isEnabled);
        if (isEnabled) {
            binding.btnRegister.setEnabled(true);
        } else {
            binding.btnRegister.setEnabled(false);
        }
    }

    private void submitForm(String email, String username, String password) {

        // Lempar data ke Activity berikutnya
        User tempUser = new User();
        tempUser.setUsername(username);
        tempUser.setEmail(email);
        tempUser.setPassword(password);
        tempUser.setPhoneNumber(phoneNumber);

        if (isGoogleSignup) {
            authViewModel.saveUserToFirestoreAfterAuth(FirebaseAuth.getInstance().getUid(), tempUser);
            authViewModel.getUserLiveData().observe(this, firebaseUser -> {
                if (firebaseUser != null) {
                    Toast.makeText(this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show();

                    // Pindah ke Homepage
                    Intent intent = new Intent(RegisterActivity.this, HomepageActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
        Intent intent = new Intent(RegisterActivity.this, RegisterSelectVerificationActivity.class);
        intent.putExtra("EXTRA_USER_DATA", tempUser);
        startActivity(intent);
        }
    }

    // TextWatcher dummy agar perubahan nama/email juga memicu validasi tombol
    private final TextWatcher genericTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            validateInputs();
        }
    };
}