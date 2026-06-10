package com.example.florist.views.seller;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.florist.databinding.ActivitySellerOrderBinding;
import com.example.florist.viewmodels.SellerOrderViewModel;
import com.google.android.material.tabs.TabLayoutMediator;

public class SellerOrderActivity extends AppCompatActivity {

    private ActivitySellerOrderBinding binding;
    private SellerOrderViewModel viewModel;
    private ProgressDialog progressDialog;

    private final String[] tabTitles = new String[]{"Pesanan Baru", "Diproses", "Dikirim", "Selesai", "Dibatalkan"};
    private final String[] orderStatuses = new String[]{"Menunggu Konfirmasi", "Diproses", "Dikirim", "Selesai", "Dibatalkan"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SellerOrderViewModel.class);

        setupUI();
        setupObservers();

    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Memuat data...");
        progressDialog.setCancelable(false);

        SellerPagerAdapter pagerAdapter = new SellerPagerAdapter(this);
        binding.viewPagerSellerOrders.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.tabLayoutSellerOrders, binding.viewPagerSellerOrders,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        int targetTab = getIntent().getIntExtra("TAB_INDEX", 0);
        binding.viewPagerSellerOrders.setCurrentItem(targetTab, false);
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) progressDialog.show();
            else progressDialog.dismiss();
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getActionSuccessMessage().observe(this, message -> {
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private class SellerPagerAdapter extends FragmentStateAdapter {

        public SellerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return SellerOrderFragment.newInstance(orderStatuses[position]);
        }

        @Override
        public int getItemCount() {
            return tabTitles.length;
        }
    }
}