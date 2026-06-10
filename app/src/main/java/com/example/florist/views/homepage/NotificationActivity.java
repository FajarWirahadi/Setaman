package com.example.florist.views.homepage; // Sesuaikan package Anda

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.NotificationAdapter;
import com.example.florist.databinding.ActivityNotificationBinding;
import com.example.florist.viewmodels.NotificationViewModel;
import com.example.florist.views.buyer.BuyerOrderDetailActivity;

public class NotificationActivity extends AppCompatActivity {

    private ActivityNotificationBinding binding;
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        setupUI();
        setupObservers();

        // Mulai memuat notifikasi
        viewModel.loadMyNotifications();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnShopNow.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, com.example.florist.views.homepage.HomepageActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        adapter = new NotificationAdapter(notification -> {

            if (!notification.isRead()) {
                viewModel.markNotificationAsRead(notification.getNotificationId());
            }

            String type = notification.getType() != null ? notification.getType() : "";
            if (type.equals("TRANSACTION")) {
                 Intent intent = new Intent(this, BuyerOrderDetailActivity.class);
                 intent.putExtra("EXTRA_ORDER_ID", notification.getRelatedId());
                 startActivity(intent);
                Toast.makeText(this, "Membuka pesanan: " + notification.getRelatedId(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, notification.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        binding.rvNotifications.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.rvNotifications.setAdapter(adapter);
    }

    private void setupObservers() {
        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getNotifications().observe(this, notifications -> {
            binding.progressBar.setVisibility(View.GONE);

            if (notifications != null && !notifications.isEmpty()) {
                adapter.setNotifications(notifications);
                binding.rvNotifications.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.rvNotifications.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            binding.progressBar.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}