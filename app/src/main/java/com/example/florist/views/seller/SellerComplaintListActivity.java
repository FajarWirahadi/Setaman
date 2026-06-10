package com.example.florist.views.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.adapter.SellerComplaintAdapter;
import com.example.florist.databinding.ActivitySellerComplaintListBinding;
import com.example.florist.viewmodels.SellerComplaintViewModel;
import com.example.florist.views.LoginActivity;
// IMPORT FIREBASE AUTH DIHAPUS!

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

        adapter = new SellerComplaintAdapter(this, rental -> {
            Intent intent = new Intent(this, RentalDetailActivity.class);
            intent.putExtra("RENTAL_ID", rental.getRentalId());
            intent.putExtra("ORDER_ID", rental.getOrderId());
            intent.putExtra("ROLE", "SELLER");
            intent.putExtra("STORE_NAME", rental.getSellerName());
            intent.putExtra("STORE_PHOTO_URL", "");
            startActivity(intent);
        });
        binding.rvComplaints.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getIsSessionExpired().observe(this, isExpired -> {
            if (isExpired != null && isExpired) {
                Toast.makeText(this, "Sesi berakhir, silakan masuk ulang", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility((isLoading != null && isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getComplaintRentals().observe(this, rentals -> {
            if (rentals != null) adapter.updateList(rentals);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }
}