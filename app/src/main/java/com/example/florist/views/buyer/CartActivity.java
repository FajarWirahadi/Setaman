package com.example.florist.views.buyer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.florist.R;
import com.example.florist.adapter.CartAdapter;
import com.example.florist.databinding.ActivityCartBinding;
import com.example.florist.databinding.DialogEditDurationBinding;
import com.example.florist.model.CartItem;
import com.example.florist.viewmodels.CartViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartClickListener {

    private ActivityCartBinding binding;
    private CartAdapter cartAdapter;
    private CartViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);

        setupUI();
        setupObservers();

        viewModel.listenToMyCart();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup RecyclerView
        cartAdapter = new CartAdapter(this, new ArrayList<>(), this);
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCartItems.setAdapter(cartAdapter);

        // Tombol Checkout
        binding.btnCheckout.setOnClickListener(v -> {
            if (cartAdapter.getItemCount() == 0) {
                Toast.makeText(this, "Keranjangmu masih kosong!", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, CheckoutActivity.class));
            }
        });
    }

    private void setupObservers() {
        // Pantau Daftar Keranjang
        viewModel.getCartItems().observe(this, items -> {
            if (items != null) {
                cartAdapter.updateList(items);
                checkEmptyState(items.isEmpty());
            }
        });

        // Pantau Total Harga
        viewModel.getTotalPrice().observe(this, total -> {
            if (total != null) {
                NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                formatRupiah.setMaximumFractionDigits(0);
                binding.tvTotalPrice.setText(formatRupiah.format(total));
            }
        });

        // Pantau Pesan Aksi (Toast)
        viewModel.getActionMessage().observe(this, message -> {
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void checkEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.rvCartItems.setVisibility(View.GONE);
            binding.layoutEmptyCart.setVisibility(View.VISIBLE);
            binding.btnCheckout.setEnabled(false);
            binding.btnCheckout.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_400));
        } else {
            binding.rvCartItems.setVisibility(View.VISIBLE);
            binding.layoutEmptyCart.setVisibility(View.GONE);
            binding.btnCheckout.setEnabled(true);
            binding.btnCheckout.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
        }
    }

    // ==========================================
    // DELEGASI KLIK DARI ADAPTER KE VIEWMODEL
    // ==========================================
    @Override
    public void onPlusClick(CartItem item, int position) {
        viewModel.updateQuantity(item, item.getQuantity() + 1);
    }

    @Override
    public void onMinusClick(CartItem item, int position) {
        if (item.getQuantity() > 1) {
            viewModel.updateQuantity(item, item.getQuantity() - 1);
        }
    }

    @Override
    public void onDeleteClick(CartItem item, int position) {
        viewModel.deleteItem(item);
    }

    @Override
    public void onEditDurationClick(CartItem item, int position) {
        showEditDurationDialog(item);
    }

    // ==========================================
    // BOTTOM SHEET UX KELAS ATAS (Tanpa Firebase)
    // ==========================================
    private void showEditDurationDialog(CartItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogEditDurationBinding dialogBinding = DialogEditDurationBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        final int[] tempDurationValue = {item.getDurationValue()};
        final String[] tempDurationType = {item.getDurationType()};

        dialogBinding.tvEditDurationValue.setText(String.valueOf(tempDurationValue[0]));

        Runnable updateColors = () -> {
            dialogBinding.btnEditTypeHarian.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            dialogBinding.btnEditTypeHarian.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            dialogBinding.btnEditTypeMingguan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            dialogBinding.btnEditTypeMingguan.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            dialogBinding.btnEditTypeBulanan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            dialogBinding.btnEditTypeBulanan.setTextColor(ContextCompat.getColor(this, R.color.gray_700));

            if (tempDurationType[0].equals("Harian")) {
                dialogBinding.btnEditTypeHarian.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnEditTypeHarian.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else if (tempDurationType[0].equals("Mingguan")) {
                dialogBinding.btnEditTypeMingguan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnEditTypeMingguan.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else if (tempDurationType[0].equals("Bulanan")) {
                dialogBinding.btnEditTypeBulanan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnEditTypeBulanan.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        };
        updateColors.run();

        dialogBinding.btnEditMinDuration.setOnClickListener(v -> {
            if (tempDurationValue[0] > 1) {
                tempDurationValue[0]--;
                dialogBinding.tvEditDurationValue.setText(String.valueOf(tempDurationValue[0]));
            }
        });

        dialogBinding.btnEditAddDuration.setOnClickListener(v -> {
            tempDurationValue[0]++;
            dialogBinding.tvEditDurationValue.setText(String.valueOf(tempDurationValue[0]));
        });

        dialogBinding.btnEditTypeHarian.setOnClickListener(v -> { tempDurationType[0] = "Harian"; updateColors.run(); });
        dialogBinding.btnEditTypeMingguan.setOnClickListener(v -> { tempDurationType[0] = "Mingguan"; updateColors.run(); });
        dialogBinding.btnEditTypeBulanan.setOnClickListener(v -> { tempDurationType[0] = "Bulanan"; updateColors.run(); });

        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveDuration.setOnClickListener(v -> {
            // SURUH VIEWMODEL YANG BEKERJA MENGUPDATE DATABASE!
            viewModel.updateDuration(item, tempDurationValue[0], tempDurationType[0]);
            dialog.dismiss();
        });

        dialog.show();
    }
}