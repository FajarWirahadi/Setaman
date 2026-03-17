package com.example.florist.views.seller.createshop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.florist.R;
import com.example.florist.databinding.ActivityShopGeneralInfoBinding;
import com.example.florist.utils.MyUtils;
import com.example.florist.viewmodels.SellerViewModel;

public class ShopGeneralInfoActivity extends AppCompatActivity {
    private ActivityShopGeneralInfoBinding binding;

    Button btnNext;
    ImageView btnBack;
    EditText editTextShopName, editTextUsername;

    SellerViewModel sViewModel;
    MyUtils myUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopGeneralInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        editTextShopName = findViewById(R.id.editTextShopName);
        editTextUsername = findViewById(R.id.editTextUsername);

        setupToolbar();
        setupInputValidation();

        binding.btnNext.setOnClickListener(v-> {
            String shopName = binding.editTextShopName.getText().toString().trim();
            String ownerName = binding.editTextUsername.getText().toString().trim();

            if (shopName.length() < 3 || ownerName.length() < 3) {
                Toast.makeText(this, "Nama Toko & Pemilik minimal 3 huruf", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ShopGeneralInfoActivity.this, ShopAddressActivity.class);
            intent.putExtra("SHOP_NAME", shopName);
            intent.putExtra("OWNER_NAME", ownerName);
            startActivity(intent);
        });


        new MyUtils.MultiTextWatcher()
                .registerEditText(editTextShopName)
                .registerEditText(editTextUsername)
                        .setCallback(new MyUtils.MultiTextWatcher.TextWatcherWithInstance() {
                            @Override
                            public void beforeTextChanged(EditText editText, CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(EditText editText, CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(EditText editText, Editable editable) {
                            String text = editable.toString();
                            String text2 = editText.toString();



                                if (text.length() >=6 && text2.length() >=6) {

                            } else {
                                btnNext.setBackground(AppCompatResources.getDrawable(ShopGeneralInfoActivity.this, R.drawable.rounded_gray_button));

                            }
                            }
                        });

    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbarTitle.setText("Buka toko");
        binding.btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupInputValidation() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                validateForm();
            }
        };
        binding.editTextShopName.addTextChangedListener(watcher);
        binding.editTextUsername.addTextChangedListener(watcher);

    }

    private void validateForm() {
        String shopName = binding.editTextShopName.getText().toString().trim();
        String ownerName = binding.editTextUsername.getText().toString().trim();

        boolean isValid = shopName.length() > 3 && ownerName.length() > 3;

        if (isValid) {
            binding.btnNext.setEnabled(true);
        } else {
            binding.btnNext.setEnabled(false);
        }
    }
}