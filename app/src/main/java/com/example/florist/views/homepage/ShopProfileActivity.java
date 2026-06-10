package com.example.florist.views.homepage;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ActivityShopProfileBinding;
import com.example.florist.model.Product;
import com.example.florist.viewmodels.CartViewModel;
import com.example.florist.viewmodels.ShopProfileViewModel;
import com.example.florist.views.buyer.CartActivity;
import com.example.florist.views.chat.ChatRoomActivity;
import com.google.android.material.tabs.TabLayoutMediator;

public class ShopProfileActivity extends AppCompatActivity {

    private Product product;

    private ActivityShopProfileBinding binding;
    private ShopProfileViewModel viewModel;
    private CartViewModel cartViewModel;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ShopProfileViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        String shopId = getIntent().getStringExtra("EXTRA_SHOP_ID");
        product = (Product) getIntent().getSerializableExtra("EXTRA_PRODUCT");


        if (shopId == null || shopId.isEmpty()) {
            Toast.makeText(this, "Data toko tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupObservers();

        cartViewModel.loadCartCount();
        viewModel.loadShopProfile(shopId);
    }

    private void setupUI() {
        String shopId = getIntent().getStringExtra("EXTRA_SHOP_ID");
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        binding.btnChat.setOnClickListener(v -> {
            if (shopId != null && !shopId.isEmpty()) {
                String shopName = binding.tvShopName.getText().toString();
                Intent intent = new Intent(ShopProfileActivity.this, ChatRoomActivity.class);
                intent.putExtra("EXTRA_TARGET_ID", shopId);

                intent.putExtra("EXTRA_TARGET_NAME", shopName.isEmpty() ? "Penjual Setaman" : shopName);
                intent.putExtra("EXTRA_TARGET_IMAGE", "");

                startActivity(intent);
            } else {
                Toast.makeText(this, "Data toko belum siap, tunggu sebentar.", Toast.LENGTH_SHORT).show();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Memuat etalase...");
        progressDialog.setCancelable(false);

        ShopPagerAdapter pagerAdapter = new ShopPagerAdapter(this);
        binding.viewPagerShop.setAdapter(pagerAdapter);


        new TabLayoutMediator(binding.shopTabLayout, binding.viewPagerShop,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Beranda");
                            tab.setIcon(R.drawable.building);
                            break;
                        case 1:
                            tab.setText("Produk");
                            tab.setIcon(R.drawable.ic_receipt);
                            break;
                        case 2:
                            tab.setText("Kategori");
                            tab.setIcon(R.drawable.building);
                            break;
                        case 3:
                            tab.setText("Ulasan");
                            tab.setIcon(R.drawable.ic_receipt);
                            break;
                    }
                }
        ).attach();
    }

    private void setupObservers() {
        cartViewModel.getCartBadgeCount().observe(this, this::updateCartBadgeUI);
        viewModel.getShopData().observe(this, shop -> {
            if (shop != null) {
                binding.tvShopName.setText(shop.getShopName());
                binding.tvShopFollowerCount.setText(shop.getShopCity() != null ? "Kota " + shop.getShopCity() : "Lokasi tidak diketahui");

                if (shop.getShopImageUrl() != null && !shop.getShopImageUrl().isEmpty()) {
                    Glide.with(this)
                            .load(shop.getShopImageUrl())
                            .placeholder(R.color.gray_100)
                            .error(R.drawable.building)
                            .circleCrop()
                            .into(binding.imgShopProfile);
                } else {
                    Glide.with(this).load(R.drawable.building).circleCrop().into(binding.imgShopProfile);
                }
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) progressDialog.show();
            else progressDialog.dismiss();
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCartBadgeUI(int count) {
        if (count > 0) {
            binding.tvCartBadgeCount.setVisibility(View.VISIBLE);
            binding.tvCartBadgeCount.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            binding.tvCartBadgeCount.setVisibility(View.GONE);
        }
    }

    private class ShopPagerAdapter extends FragmentStateAdapter {

        private static final int NUM_TABS = 4;

        public ShopPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            return ShopProductFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return NUM_TABS;
        }
    }
}