package com.example.florist.views.seller;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.florist.databinding.ActivityProductDetailBinding;
import com.example.florist.model.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    private ActivityProductDetailBinding binding;
    private Product product;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        product = (Product) getIntent().getSerializableExtra("EXTRA_PRODUCT");
        if (product == null) {
            finish();
            return;
        }

        setupUI();
        setupImageSliders();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> {finish();});

        binding.btnChat.setOnClickListener(v -> {
            Toast.makeText(this, "Chat Penjual", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddToCart.setOnClickListener(v -> {
            Toast.makeText(this, "Masukkan Keranjang", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnBuyNow.setOnClickListener(v -> {
            Toast.makeText(this, "Beli Sekarang", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupImageSliders() {
        List<String> images = new ArrayList<>();

        if (product.getGallery() != null && !product.getGallery().isEmpty()) {
            images.addAll(product.getGallery());
        } else if (product.getImageUrl() != null) {
            images.add(product.getImageUrl());
        }

        com.example.florist.adapter.ImageSliderAdapter adapter = new com.example.florist.adapter.ImageSliderAdapter(this, images);
        binding.viewPagerSlider.setAdapter(adapter);

        updateIndicator(0, images.size());

        binding.viewPagerSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position, images.size());
            }
        });
    }

    private void updateIndicator(int i, int size) {
        binding.tvImageIndicator.setText((i + 1) + "/" + size);
    }


    private void setupUI() {
        binding.tvProductName.setText(product.getName());
        binding.tvDescription.setText(product.getDescription());;

        NumberFormat formatRp = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        String price = formatRp.format(product.getPrice());
        if (price.endsWith(",00")) price = price.substring(0, price.length() -3);
        binding.tvProductPrice.setText(price + "/" + product.getDuration());

        binding.tvRating.setText(" 4.5/5 (20) | ");
        binding.tvSold.setText("20 Tersewa");

    }
}