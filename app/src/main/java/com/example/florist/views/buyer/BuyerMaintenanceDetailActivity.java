package com.example.florist.views.buyer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.florist.adapter.BuyerMaintenanceAdapter;
import com.example.florist.databinding.ActivityBuyerMaintenanceDetailBinding;
import com.example.florist.databinding.DialogBuyerComplaintBinding;
import com.example.florist.viewmodels.BuyerMaintenanceViewModel;
import com.example.florist.viewmodels.ComplaintViewModel;
import com.example.florist.views.homepage.HomepageActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;

public class BuyerMaintenanceDetailActivity extends AppCompatActivity {
    private ActivityBuyerMaintenanceDetailBinding binding;
    private BuyerMaintenanceAdapter adapter;
    private BuyerMaintenanceViewModel viewModel;
    private ComplaintViewModel complaintViewModel;
    private BottomSheetDialog complaintDialog;
    private Uri complaintImageUri = null;
    private DialogBuyerComplaintBinding complaintBinding;
    private String orderId;


    private final ActivityResultLauncher<PickVisualMediaRequest> pickComplaintMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    complaintImageUri = uri;
                    if (complaintBinding != null) {
                        Glide.with(this).load(uri).into(complaintBinding.imgComplaintPreview);
                    }
                    checkComplaintValidation();
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuyerMaintenanceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) {
            Toast.makeText(this, "Data pesanan tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        viewModel = new ViewModelProvider(this).get(BuyerMaintenanceViewModel.class);
        complaintViewModel = new ViewModelProvider(this).get(ComplaintViewModel.class);
        setupUI();
        setupObservers();

        viewModel.startListening(orderId);
    }
    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        adapter = new BuyerMaintenanceAdapter(log -> {

            String message = "Halo, saya ingin bertanya tentang perawatan tanaman saya pada tanggal " +
                    new SimpleDateFormat("dd MMM").format(log.getCreatedAt().toDate()) +
                    ". Berikut catatan perawatannya: " + log.getDescription();

            openWhatsAppOrChat(message);
        });
        binding.rvTimeline.setAdapter(adapter);

        binding.btnSubmitComplaint.setOnClickListener(v -> showComplaintDialog());
    }

    private void setupObservers() {
        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getMaintenanceLogs().observe(this, logs -> {
            binding.progressBar.setVisibility(View.GONE);
            if (logs != null && !logs.isEmpty()) {
                adapter.setLogs(logs);
            } else {
                Toast.makeText(this, "Belum ada riwayat perawatan", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            binding.progressBar.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        complaintViewModel.getIsSuccess().observe(this, task -> {
            if (task != null) {
                binding.progressBar.setVisibility(View.GONE);
                complaintDialog.dismiss();
                Toast.makeText(this, "Berhasil mengajukan komplain", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, HomepageActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openWhatsAppOrChat(String message) {
        // TODO: Eksekusi intent ke WhatsApp atau in-app chat
        Toast.makeText(this, "Membuka obrolan: " + message, Toast.LENGTH_SHORT).show();
    }

    private void checkComplaintValidation() {
        if (complaintBinding != null) {
            String text = complaintBinding.etComplaintDescription.getText().toString().trim();
            boolean isValid = (complaintImageUri != null) && (!text.isEmpty());
            complaintBinding.btnSubmitComplaint.setEnabled(isValid);
        }
    }

    private void showComplaintDialog() {
        complaintDialog = new BottomSheetDialog(this);
        complaintBinding = DialogBuyerComplaintBinding.inflate(getLayoutInflater());
        complaintDialog.setContentView(complaintBinding.getRoot());

        complaintImageUri = null;

        String[] reasons = {"Tanaman Layu/Mati", "Pot Pecah/Rusak", "Perawatan Tidak Sesuai", "Lainnya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reasons);
        complaintBinding.spinnerReason.setAdapter(adapter);

        complaintBinding.imgComplaintPreview.setOnClickListener(v -> {
            pickComplaintMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        complaintBinding.etComplaintDescription.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkComplaintValidation(); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        complaintBinding.btnSubmitComplaint.setOnClickListener(v -> {
            String reason = complaintBinding.spinnerReason.getSelectedItem().toString();
            String desc = complaintBinding.etComplaintDescription.getText().toString().trim();

            complaintViewModel.submitComplaint(orderId, reason, desc, complaintImageUri);
        });

        complaintDialog.show();
    }
}