package com.example.florist.views.buyer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.florist.adapter.MyOrdersPagerAdapter;
import com.example.florist.databinding.ActivityMyOrdersBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class MyOrdersActivity extends AppCompatActivity {

    private ActivityMyOrdersBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        MyOrdersPagerAdapter pagerAdapter = new MyOrdersPagerAdapter(this);
        binding.viewPagerOrders.setAdapter(pagerAdapter);

        String[] tabTitles = {"Menunggu", "Diproses", "Dikirim", "Selesai", "Dibatalkan"};

        new TabLayoutMediator(binding.tabLayoutOrders, binding.viewPagerOrders,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // 1. TANGKAP DATA INTENT
        int targetTab = getIntent().getIntExtra("TAB_INDEX", 0);

        // 2. ALAT DIAGNOSTIK: Munculkan pesan untuk mengecek angka yang diterima
        android.widget.Toast.makeText(this, "Target Tab: " + targetTab, android.widget.Toast.LENGTH_SHORT).show();

        // 3. BRUTE-FORCE DELAY: Paksa Android menunggu 150 milidetik
        // sebelum memindahkan layar, memastikan ViewPager2 sudah 100% jadi.
        binding.viewPagerOrders.postDelayed(() -> {
            binding.viewPagerOrders.setCurrentItem(targetTab, false);
        }, 150);
    }
}