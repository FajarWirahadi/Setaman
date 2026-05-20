package com.example.florist.views.buyer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;


import com.example.florist.adapter.CheckoutAdapter;
import com.example.florist.databinding.ActivityCheckoutBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.DeliveryAddress;
import com.example.florist.viewmodels.CheckoutViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private CheckoutViewModel viewModel;
    private CheckoutAdapter adapter;
    private ProgressDialog progressDialog;
    private final ActivityResultLauncher<Intent> addressLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            DeliveryAddress selected = (DeliveryAddress) result.getData().getSerializableExtra("SELECTED_ADDRESS");
                            if (selected != null) {
                                viewModel.setSelectedAddress(selected);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CheckoutViewModel.class);

        setupUI();
        setupObservers();

        if (getIntent().hasExtra("EXTRA_DIRECT_BUY_ITEM")) {
            CartItem singleItem = (CartItem) getIntent().getSerializableExtra("EXTRA_DIRECT_BUY_ITEM");
            viewModel.setDirectBuyItem(singleItem);
        }

        viewModel.loadInitialData();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        adapter = new CheckoutAdapter(this, new ArrayList<>());
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(adapter);

        binding.layoutAddressSelection.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressSelectionActivity.class);
            addressLauncher.launch(intent);
        });

        binding.btnPlaceOrder.setOnClickListener(v -> {
            int selectedPaymentId = binding.rgPaymentMethod.getCheckedRadioButtonId();
            RadioButton selectedRb = findViewById(selectedPaymentId);
            if (selectedRb != null) {
                viewModel.processOrder(selectedRb.getText().toString());
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang memproses pesananmu...");
        progressDialog.setCancelable(false);
    }

    private void setupObservers() {
        viewModel.getCheckoutList().observe(this, items -> {
            adapter.updateData(items);
        });

        viewModel.getSelectedAddress().observe(this, address -> {
            if (address != null) {
                binding.tvReceiverInfo.setText(address.getReceiverName() + " (" + address.getPhoneNumber() + ")");
                binding.tvDeliveryAddress.setText(address.getFullAddress());
            } else {
                binding.tvReceiverInfo.setText("Belum ada alamat");
                binding.tvDeliveryAddress.setText("Klik di sini untuk memilih atau menambahkan alamat pengiriman.");
            }
        });

        viewModel.getMidtransToken().observe(this, token -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (token != null && !token.isEmpty()) {
                String snapUrl = "https://app.sandbox.midtrans.com/snap/v2/vtweb/" + token;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(snapUrl));
                startActivity(browserIntent);

                Toast.makeText(CheckoutActivity.this, "Membuka gerbang pembayaran...", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(CheckoutActivity.this, "SISTEM GAGAL: Token tidak didapatkan dari server!", Toast.LENGTH_LONG).show();
            }
        });

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);

        viewModel.getSubTotal().observe(this, total -> binding.tvCheckoutSubtotal.setText(formatRupiah.format(total)));
        viewModel.getShippingCost().observe(this, cost -> binding.tvCheckoutShipping.setText(formatRupiah.format(cost)));
        viewModel.getGrandTotal().observe(this, grand -> binding.tvCheckoutFinalTotal.setText(formatRupiah.format(grand)));

        // Pantau Loading & Status
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) progressDialog.show();
            else progressDialog.dismiss();
        });

        viewModel.getIsOrderSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Hore! Pesanan Berhasil Dibuat 🎉", Toast.LENGTH_LONG).show();
                finish(); // Keluar dari kasir
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }
}