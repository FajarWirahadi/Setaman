package com.example.florist.views.buyer;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.OrderItemAdapter;
import com.example.florist.adapter.TrackingAdapter;
import com.example.florist.databinding.ActivityBuyerOrderDetailBinding;
import com.example.florist.databinding.DialogBuyerTrackingBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Order;
import com.example.florist.viewmodels.BuyerOrderDetailViewModel;
import com.example.florist.viewmodels.BuyerTrackingViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BuyerOrderDetailActivity extends AppCompatActivity {

    private ActivityBuyerOrderDetailBinding binding;
    private String orderId;

    private BuyerOrderDetailViewModel detailViewModel;
    private BuyerTrackingViewModel trackingViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuyerOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("EXTRA_ORDER_ID");
        if (orderId == null) {
            Toast.makeText(this, "Order ID tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        detailViewModel = new ViewModelProvider(this).get(BuyerOrderDetailViewModel.class);
        trackingViewModel = new ViewModelProvider(this).get(BuyerTrackingViewModel.class);

        setupUI();
        setupObservers();

        detailViewModel.fetchOrderDetail(orderId);
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnTrackOrder.setOnClickListener(v -> showBuyerTrackingDialog(orderId));

        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupObservers() {
        detailViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.tvDetailStatus.setText("Memuat...");
            }
        });

        detailViewModel.getOrderDetail().observe(this, order -> {
            if (order != null) {
                populateUI(order);
            }
        });

        detailViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(Order order) {
        String status = order.getStatus();
        binding.tvDetailStatus.setText(status);

        if ("Diproses".equalsIgnoreCase(status) || "Dikirim".equalsIgnoreCase(status)) {
            binding.btnTrackOrder.setVisibility(View.VISIBLE);
        } else {
            binding.btnTrackOrder.setVisibility(View.GONE);
        }

        binding.tvBuyerName.setText(order.getReceiverName());
        binding.tvBuyerAddress.setText(order.getFullDeliveryAddress());

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
        binding.tvDetailTotalAmount.setText(formatRupiah.format(order.getTotalAmount()));

        List<CartItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(this, items);
            binding.rvOrderItems.setAdapter(adapter);
        }

        if ("PENDING".equalsIgnoreCase(status)) {
            binding.btnPrimaryAction.setVisibility(View.VISIBLE);
            binding.btnPrimaryAction.setText("Bayar Sekarang");
        } else if ("Dikirim".equalsIgnoreCase(status)) {
            binding.btnPrimaryAction.setVisibility(View.VISIBLE);
            binding.btnPrimaryAction.setText("Konfirmasi Pesanan Diterima");
        } else {
            binding.btnPrimaryAction.setVisibility(View.GONE);
        }
    }

    private void showBuyerTrackingDialog(String orderId) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogBuyerTrackingBinding dialogBinding = DialogBuyerTrackingBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        TrackingAdapter adapter = new TrackingAdapter();
        dialogBinding.rvTrackingHistory.setLayoutManager(new LinearLayoutManager(this));
        dialogBinding.rvTrackingHistory.setAdapter(adapter);

        trackingViewModel.fetchTrackingHistory(orderId);
        trackingViewModel.getTrackingLogs().observe(this, logs -> {
            if (logs != null && !logs.isEmpty()) {
                adapter.setTrackingData(logs);
            }
        });

        dialog.show();
    }
}