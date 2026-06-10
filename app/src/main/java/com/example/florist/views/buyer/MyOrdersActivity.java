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

        int targetTab = getIntent().getIntExtra("TAB_INDEX", 0);
        binding.viewPagerOrders.setCurrentItem(targetTab, false);
    }
}