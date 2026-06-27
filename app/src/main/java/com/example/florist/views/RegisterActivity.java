package com.example.florist.views;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.ActivityRegisterBinding;
import com.example.florist.model.User;
import com.example.florist.repository.AuthRepository;
import com.example.florist.viewmodels.AuthViewModel;
import com.example.florist.views.homepage.HomepageActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private String phoneNumber;
    private AuthViewModel authViewModel;
    private boolean isGoogleSignup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ObjectAnimator.ofInt(binding.progressBar, "progress", 25, 50).setDuration(1000).start();

        fetchIntentData();
        setupListeners();
        observeViewModel();

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void fetchIntentData() {
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
    }

    private void setupListeners() {
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                authViewModel.registerDataChanged(
                        binding.editTextUserName.getText().toString(),
                        binding.editTextEmail.getText().toString(),
                        binding.editTextPasswordRegister2.getText().toString(), // Perhatikan ID-mu terbalik di XML sebelumnya, ini password utama
                        binding.editTextPasswordRegister.getText().toString()   // Ini confirm password
                );
            }
        };

        binding.editTextUserName.addTextChangedListener(afterTextChangedListener);
        binding.editTextEmail.addTextChangedListener(afterTextChangedListener);
        binding.editTextPasswordRegister.addTextChangedListener(afterTextChangedListener);
        binding.editTextPasswordRegister2.addTextChangedListener(afterTextChangedListener);

        binding.textViewShowPassword.setOnClickListener(v -> authViewModel.toggleConfirmPasswordVisibility());
        binding.textViewShowPassword2.setOnClickListener(v -> authViewModel.togglePasswordVisibility());

        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.editTextUserName.getText().toString().trim();
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPasswordRegister2.getText().toString().trim();

            authViewModel.checkEmailAvailability(email, new AuthRepository.CheckEmailAvailability() {
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
    }

    private void observeViewModel() {
        // Observasi Form State
        authViewModel.getRegisterFormState().observe(this, formState -> {
            if (formState == null) return;

            binding.btnRegister.setEnabled(formState.isDataValid());

            updateCriteriaStyle(formState.hasMinLength(), binding.minimumCharPasswordCheck, binding.minimumCharPassword);
            updateCriteriaStyle(formState.hasUppercase(), binding.uppercaseCharPasswordCheck, binding.uppercaseCharPassword);
            updateCriteriaStyle(formState.hasNumber(), binding.minimumNumberPasswordCheck, binding.minimumNumberPassword);

            boolean isPasswordValid = formState.hasMinLength() && formState.hasUppercase() && formState.hasNumber();

            binding.editTextPasswordRegister2.setBackground(AppCompatResources.getDrawable(this,
                    isPasswordValid ? R.drawable.rounded_success_edittext : R.drawable.rounded_normal_edittext));

            binding.editTextPasswordRegister.setBackground(AppCompatResources.getDrawable(this,
                    formState.isConfirmValid() ? R.drawable.rounded_success_edittext : R.drawable.rounded_normal_edittext));
        });

        // Observasi Visibility Password Utama
        authViewModel.getIsPasswordVisible().observe(this, isVisible -> {
            updatePasswordVisibilityUI(binding.editTextPasswordRegister2, binding.textViewShowPassword2, isVisible);
        });

        // Observasi Visibility Confirm Password
        authViewModel.getIsConfirmPasswordVisible().observe(this, isVisible -> {
            updatePasswordVisibilityUI(binding.editTextPasswordRegister, binding.textViewShowPassword, isVisible);
        });
    }

    private void updatePasswordVisibilityUI(EditText editText, TextView toggleView, boolean isVisible) {
        if (isVisible) {
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            toggleView.setText("Tutup");
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            toggleView.setText("Lihat");
        }
        editText.setSelection(editText.getText().length());
    }

    private void updateCriteriaStyle(boolean isValid, ImageView iconView, TextView textView) {
        if (isValid) {
            iconView.setImageResource(R.drawable.success_check);
            textView.setTextColor(getResources().getColor(R.color.olive_500));
        } else {
            iconView.setImageResource(R.drawable.normal_check);
            textView.setTextColor(getResources().getColor(R.color.gray_500));
        }
    }

    private void submitForm(String email, String username, String password) {
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
}