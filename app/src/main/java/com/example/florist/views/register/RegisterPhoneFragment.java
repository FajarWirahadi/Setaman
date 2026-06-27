package com.example.florist.views.register;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.florist.R;
import com.example.florist.viewmodels.RegisterSharedViewModel;

public class RegisterPhoneFragment extends Fragment {

    private RegisterSharedViewModel sharedViewModel;

    // Sesuai dengan ID di XML milikmu
    private EditText etPhoneNumber;
    private Button btnRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_phone, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        btnRegister = view.findViewById(R.id.btnRegister);
        textViewLogin = view.findViewById(R.id.textViewLogin);
        progressBar = view.findViewById(R.id.progress_bar);

        // Ambil ViewModel dari Inang (RegisterActivity)
        sharedViewModel = new ViewModelProvider(requireActivity()).get(RegisterSharedViewModel.class);

        setupInputValidation();
        setupObservers();
        setupClickListeners();
    }

    private void setupInputValidation() {
        // Logika untuk menghidupkan tombol hanya jika ada input
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Asumsi dasar: nomor HP minimal 9 digit
                btnRegister.setEnabled(s.toString().trim().length() >= 9);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> {
            String phone = etPhoneNumber.getText().toString().trim();

            // Format nomor akan digabung dengan +62, pastikan tidak ada double 0 di depan
            if (phone.startsWith("0")) {
                phone = phone.substring(1);
            }
            String finalPhone = "+62" + phone;

            sharedViewModel.clearErrors();
            sharedViewModel.setPhoneNumberAndCheck(finalPhone);
        });

        textViewLogin.setOnClickListener(v -> {
            // Karena ini pintu masuk pendaftaran, jika klik login, tutup saja Activity ini
            requireActivity().finish();
        });
    }

    private void setupObservers() {

        // 1. Pantau status Loading dengan ProgressBar
        sharedViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                btnRegister.setEnabled(!isLoading);

                // Tampilkan progress bar dan sembunyikan teks tombol (atau biarkan teksnya, terserah seleramu)
                if (isLoading) {
                    progressBar.setVisibility(View.VISIBLE);
                    btnRegister.setText("Memeriksa...");
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setText(getString(R.string.lanjut));
                }
            }
        });

        // 2. Pantau Error
        sharedViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                etPhoneNumber.setError(error);
                etPhoneNumber.requestFocus();
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Pantau Hasil
        sharedViewModel.getIsPhoneAvailable().observe(getViewLifecycleOwner(), isAvailable -> {
            if (isAvailable != null) {
                if (isAvailable) {
                    // Nomor aman, terbang ke halaman Profil!
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_phone_to_profile);

                    sharedViewModel.resetPhoneAvailability();
                } else {
                    etPhoneNumber.setError("Nomor HP sudah terdaftar!");
                    etPhoneNumber.requestFocus();
                }
            }
        });
    }
}