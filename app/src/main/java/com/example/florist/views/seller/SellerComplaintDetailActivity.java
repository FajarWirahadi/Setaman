package com.example.florist.views.seller;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ActivitySellerComplaintDetailBinding;
import com.example.florist.model.Complaint;
import com.example.florist.viewmodels.SellerComplaintDetailViewModel;

public class SellerComplaintDetailActivity extends AppCompatActivity {

    private ActivitySellerComplaintDetailBinding binding;
    private SellerComplaintDetailViewModel viewModel;
    private String orderId;
    private Complaint currentComplaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerComplaintDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) {
            Toast.makeText(this, "ID Pesanan tidak ditemukan.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(SellerComplaintDetailViewModel.class);

        setupUI();
        setupObservers();

        viewModel.fetchComplaintDetail(orderId);
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSubmitResolution.setOnClickListener(v -> {
            String responseText = binding.etSellerResponse.getText().toString().trim();

            if (responseText.isEmpty()) {
                Toast.makeText(this, "Tanggapan tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentComplaint != null) {
                viewModel.resolveComplaint(orderId, currentComplaint.getComplaintId(), responseText);
            }
        });
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility((isLoading != null && isLoading) ? View.VISIBLE : View.GONE);
            binding.contentLayout.setVisibility((isLoading != null && isLoading) ? View.GONE : View.VISIBLE);
        });

        viewModel.getActiveComplaint().observe(this, complaint -> {
            if (complaint != null) {
                currentComplaint = complaint;

                binding.tvComplaintReason.setText("Alasan: " + complaint.getReason());
                binding.tvComplaintDesc.setText(complaint.getDescription());

                if (complaint.getEvidenceImageUrl() != null && !complaint.getEvidenceImageUrl().isEmpty()) {
                    Glide.with(this)
                            .load(complaint.getEvidenceImageUrl())
                            .placeholder(R.drawable.rounded_gray_layout)
                            .into(binding.imgComplaintEvidence);
                }

                if ("Resolved".equals(complaint.getStatus())) {
                    binding.etSellerResponse.setText(complaint.getSellerResponseText());
                    binding.etSellerResponse.setEnabled(false);
                    binding.btnSubmitResolution.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getIsResolveSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                Toast.makeText(this, "Sengketa berhasil diselesaikan!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}