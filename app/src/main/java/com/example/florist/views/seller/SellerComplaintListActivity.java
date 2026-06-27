package com.example.florist.views.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.SellerComplaintAdapter;
import com.example.florist.databinding.ActivitySellerComplaintListBinding;
import com.example.florist.viewmodels.SellerComplaintViewModel;
import com.example.florist.views.LoginActivity;

public class SellerComplaintListActivity extends AppCompatActivity {

    private ActivitySellerComplaintListBinding binding;
    private SellerComplaintAdapter adapter;
    private SellerComplaintViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerComplaintListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SellerComplaintViewModel.class);

        setupUI();
        setupObservers();
        viewModel.fetchComplaintOrders();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SellerComplaintAdapter(this, complaint -> {
            Intent intent = new Intent(this, RentalDetailActivity.class);
            intent.putExtra("RENTAL_ID", complaint.getRentalId());
            intent.putExtra("ROLE", "SELLER");
            startActivity(intent);
        });
        binding.rvComplaints.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getIsSessionExpired().observe(this, isExpired -> {
            if (isExpired != null && isExpired) {
                Toast.makeText(this, "Sesi berakhir, silakan masuk ulang", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility((isLoading != null && isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getComplaintList().observe(this, complaints -> {
            if (complaints != null && !complaints.isEmpty()) {
                adapter.updateList(complaints);
                binding.rvComplaints.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.rvComplaints.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }
}