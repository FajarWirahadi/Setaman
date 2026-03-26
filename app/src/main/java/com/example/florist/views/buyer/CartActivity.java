package com.example.florist.views.buyer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.florist.R;
import com.example.florist.adapter.CartAdapter;
import com.example.florist.databinding.ActivityCartBinding;
import com.example.florist.databinding.DialogEditDurationBinding;
import com.example.florist.model.CartItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartClickListener {

    private ActivityCartBinding binding;
    private CartAdapter cartAdapter;
    private List<CartItem> cartList;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            currentUserId = user.getUid();
        } else {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadCartData();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        // Setup RecyclerView
        cartList = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartList, this);
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCartItems.setAdapter(cartAdapter);

        // Tombol Checkout (Nanti kita fungsikan ke halaman Pembayaran)
        binding.btnCheckout.setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Keranjangmu masih kosong!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Menuju Meja Kasir...", Toast.LENGTH_SHORT).show();
                // TODO: Pindah ke CheckoutActivity
            }
        });
    }

    // ==========================================
    // OPERASI AWAN: MENGAMBIL DATA KERANJANG
    // ==========================================
    private void loadCartData() {
        db.collection("users").document(currentUserId).collection("cart")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Gagal memuat keranjang", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        cartList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            CartItem item = doc.toObject(CartItem.class);
                            if (item != null) cartList.add(item);
                        }

                        cartAdapter.notifyDataSetChanged();
                        updateTotalPrice();
                        checkEmptyState();
                    }
                });
    }

    // ==========================================
    // MESIN PENGHITUNG TOTAL HARGA
    // ==========================================
    private void updateTotalPrice() {
        long grandTotal = 0;
        for (CartItem item : cartList) {
            int multiplier = 1;
            if ("Mingguan".equals(item.getDurationType())) multiplier = 7;
            else if ("Bulanan".equals(item.getDurationType())) multiplier = 30;

            grandTotal += (long) item.getPrice() * item.getQuantity() * item.getDurationValue() * multiplier;
        }

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
        binding.tvTotalPrice.setText(formatRupiah.format(grandTotal));
    }

    private void checkEmptyState() {
        if (cartList.isEmpty()) {
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
    // IMPLEMENTASI INTERFACE DARI ADAPTER
    // ==========================================
    @Override
    public void onPlusClick(CartItem item, int position) {
        int newQty = item.getQuantity() + 1;
        updateItemInFirestore(item.getProductId(), "quantity", newQty);
    }

    @Override
    public void onMinusClick(CartItem item, int position) {
        if (item.getQuantity() > 1) {
            int newQty = item.getQuantity() - 1;
            updateItemInFirestore(item.getProductId(), "quantity", newQty);
        }
    }

    @Override
    public void onDeleteClick(CartItem item, int position) {
        db.collection("users").document(currentUserId)
                .collection("cart").document(item.getProductId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Bunga dihapus", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onEditDurationClick(CartItem item, int position) {
        showEditDurationDialog(item);
    }

    // Fungsi Pembantu untuk Update Angka di Firestore
    private void updateItemInFirestore(String productId, String field, Object value) {
        db.collection("users").document(currentUserId)
                .collection("cart").document(productId)
                .update(field, value);
    }

    // ==========================================
    // SIHIR BOTTOM SHEET UX KELAS ATAS
    // ==========================================
    private void showEditDurationDialog(CartItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogEditDurationBinding dialogBinding = DialogEditDurationBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        // Array memori sementara untuk dialog
        final int[] tempDurationValue = {item.getDurationValue()};
        final String[] tempDurationType = {item.getDurationType()};

        dialogBinding.tvEditDurationValue.setText(String.valueOf(tempDurationValue[0]));

        // Fungsi pewarna tombol
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
        updateColors.run(); // Panggil pertama kali

        // Tombol Plus Minus Durasi
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

        // Tombol Tipe Durasi
        dialogBinding.btnEditTypeHarian.setOnClickListener(v -> { tempDurationType[0] = "Harian"; updateColors.run(); });
        dialogBinding.btnEditTypeMingguan.setOnClickListener(v -> { tempDurationType[0] = "Mingguan"; updateColors.run(); });
        dialogBinding.btnEditTypeBulanan.setOnClickListener(v -> { tempDurationType[0] = "Bulanan"; updateColors.run(); });

        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveDuration.setOnClickListener(v -> {
            db.collection("users").document(currentUserId)
                    .collection("cart").document(item.getProductId())
                    .update(
                            "durationValue", tempDurationValue[0],
                            "durationType", tempDurationType[0]
                    ).addOnSuccessListener(aVoid -> dialog.dismiss());
        });

        dialog.show();
    }
}