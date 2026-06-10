package com.example.florist.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.ActivityRegisterPhoneBinding;
import com.example.florist.repository.AuthRepository;
import com.example.florist.viewmodels.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

public class RegisterPhoneActivity extends AppCompatActivity {
    TextView textViewHintPhoneNumber, textViewLogin;
    EditText editTextPhoneNumber;
    String phoneNumber;
    int currentProgress = 0;
    int lastProgress = 0;
    private ActivityRegisterPhoneBinding binding;
    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        enableButton(false);
        setupGoogleSignIn();

        binding.etPhoneNumber.addTextChangedListener(new GenericTextWatcher(binding.etPhoneNumber));
        setupListeners();


    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> {
            String phoneNumber = binding.etPhoneNumber.getText().toString().trim();

            if (phoneNumber.isEmpty()) {
                binding.etPhoneNumber.setError("Nomor HP wajib diisi");
                return;
            }

            if (phoneNumber.length() < 10) {
                binding.etPhoneNumber.setError("Nomor HP tidak valid");
                return;
            }

            authViewModel.checkPhoneAvailability(phoneNumber, new AuthRepository.CheckPhoneAvailability() {
                @Override
                public void onResult(boolean isAvailable) {
                    if (isAvailable) {
                        Intent intent = new Intent(RegisterPhoneActivity.this, RegisterActivity.class);
                        intent.putExtra("EXTRA_PHONE", phoneNumber);
                        startActivity(intent);
                    } else {
                        binding.etPhoneNumber.setError("Coba gunakan nomor HP yang lain");
                        Snackbar.make(binding.getRoot(), "Nomor HP sudah digunakan pengguna lain", Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(RegisterPhoneActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });

        });

        authViewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.btnRegister.setEnabled(false);
                binding.btnRegister.setText("Loading");
            } else {
                binding.btnRegister.setEnabled(true);
                binding.btnRegister.setText("Masuk");
            }

        });

        binding.textViewLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        // Tombol Login Google
        binding.btnLoginGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
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


    public class GenericTextWatcher implements TextWatcher {
        private View view;
        private GenericTextWatcher(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String text = editable.toString();
            if (view.getId() == R.id.etPhoneNumber) {
                if (text.length() >= 10) {
                    enableButton(true);
                } else {
                    enableButton(false);

                }
            }

        }
    }

    private void enableButton(boolean isEnable) {
        if (isEnable) {
            // JIKA VALID (HIJAU)
            binding.etPhoneNumber.setBackground(AppCompatResources.getDrawable(this, R.drawable.rounded_success_edittext));
            binding.btnRegister.setEnabled(true); // Aktifkan tombol
        } else {
            // JIKA TIDAK VALID (ABU-ABU/NORMAL)
            binding.etPhoneNumber.setBackground(AppCompatResources.getDrawable(this, R.drawable.rounded_normal_edittext));
            binding.btnRegister.setEnabled(false); // Matikan tombol
        }
    }
}