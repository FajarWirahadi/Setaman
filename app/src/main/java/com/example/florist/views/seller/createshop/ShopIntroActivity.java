package com.example.florist.views.seller.createshop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.florist.R;
import com.example.florist.databinding.ActivityShopIntroBinding;

public class ShopIntroActivity extends AppCompatActivity {

    private ActivityShopIntroBinding binding;
    Button btnCreateShop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbarTitle.setText("Buka Toko");

        binding.btnBack.setOnClickListener(v -> {
            finish();
        });


        binding.btnCreateShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ShopIntroActivity.this, ShopGeneralInfoActivity.class);
                startActivity(intent);
            }
        });
    }
}