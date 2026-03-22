package com.example.florist.views.seller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.example.florist.adapter.ProductPagerAdapter;
import com.example.florist.databinding.ActivityMyProductBinding;
import com.example.florist.model.Product;
import com.example.florist.adapter.ProductAdapter;
import com.example.florist.viewmodels.MyProductViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyProductActivity extends AppCompatActivity {
    private ActivityMyProductBinding binding;
    private MyProductViewModel viewModel;
    ProductPagerAdapter pagerAdapter;


    private List<Product> allList = new ArrayList<>();
    private List<Product> activeList = new ArrayList<>();
    private List<Product> outOfStockList = new ArrayList<>();
    private List<Product> inactiveList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MyProductViewModel.class);
        binding.toolbar.toolbarTitle.setText("Daftar Produk");
        binding.toolbar.btnBack.setOnClickListener(v -> {finish();});

        setupViewPagerAndTabs();
        setupObservers();

        if (binding.etSearchProduct != null) {
            binding.etSearchProduct.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                    if (viewModel != null) {
                        viewModel.searchProduct(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

        if (viewModel != null) {
            viewModel.fetchMyProducts();
        }
    }


    private void setupViewPagerAndTabs() {
       pagerAdapter = new ProductPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);

        // Sihir TabLayoutMediator: Mengikat TabLayout dan ViewPager2 agar tersinkronisasi saat digeser!
        new com.google.android.material.tabs.TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    // Teks awal saat pertama kali dibuka (akan ditimpa observer)
                    if (position == 0) tab.setText("Aktif(0)");
                    else if (position == 1) tab.setText("Habis(0)");
                    else tab.setText("Nonaktif(0)");
                }
        ).attach();
    }

    private void setupObservers() {
        viewModel.getCountActive().observe(this, count -> {
            if (binding.tabLayout.getTabAt(0) != null) binding.tabLayout.getTabAt(0).setText("Aktif(" + count + ")");
        });
        viewModel.getCountSold().observe(this, count -> {
            if (binding.tabLayout.getTabAt(1) != null) binding.tabLayout.getTabAt(1).setText("Habis(" + count + ")");
        });
        viewModel.getCountInactive().observe(this, count -> {
            if (binding.tabLayout.getTabAt(2) != null) binding.tabLayout.getTabAt(2).setText("Nonaktif(" + count + ")");
        });
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                binding.loadingOverlay.setVisibility(View.VISIBLE);
            } else {
                binding.loadingOverlay.setVisibility(View.GONE);
            }
        });
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }
}