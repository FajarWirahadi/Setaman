package com.example.florist.views.homepage;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.databinding.ActivityEditProfileBinding;
import com.example.florist.viewmodels.ProfileViewModel;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private ProfileViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        String currentName = getIntent().getStringExtra("EXTRA_USERNAME");
        String currentPhone = getIntent().getStringExtra("EXTRA_PHONE");

        if (currentName != null) {
            binding.etFullName.setText(currentName);
        }
        if (currentPhone != null) {
            binding.etPhoneNumber.setText(currentPhone);
        }

        setupListeners();
        setupObservers();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnSave.setOnClickListener(v -> {
            String newName = binding.etFullName.getText().toString().trim();
            String newPhone = binding.etPhoneNumber.getText().toString().trim();

            if (newName.isEmpty()) {
                binding.etFullName.setError("Nama tidak boleh kosong");
                binding.etFullName.requestFocus();
                return;
            }

            if (newPhone.isEmpty() || newPhone.length() < 9) {
                binding.etPhoneNumber.setError("Masukkan nomor WhatsApp yang valid");
                binding.etPhoneNumber.requestFocus();
                return;
            }

            viewModel.updateProfileData(newName, newPhone);
        });
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnSave.setEnabled(false); // Matikan tombol agar tidak di-klik 2 kali
                binding.btnSave.setText("Menyimpan...");
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Simpan Perubahan");
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsUpdateSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                viewModel.resetUpdateStatus();
                finish();
            }
        });
    }
}