package com.example.florist.views.buyer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.florist.adapter.CheckoutAdapter;
import com.example.florist.databinding.ActivityCheckoutBinding;
import com.example.florist.model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private CheckoutAdapter adapter;
    private List<CartItem> checkoutList;
    private FirebaseFirestore db;
    private String currentUserId;
    private boolean isDirectBuy = false;

    private long subTotal = 0;
    private final long ONGKOS_KIRIM = 15000; // Hardcode ongkir untuk sementara
    private long grandTotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
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
        loadCheckoutData();
        loadUserProfile();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        checkoutList = new ArrayList<>();
        adapter = new CheckoutAdapter(this, checkoutList);
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(adapter);

        binding.btnPlaceOrder.setOnClickListener(v -> processOrder());
    }
    private void loadCheckoutData() {
        checkoutList.clear();
        subTotal = 0;

        // Cek apakah ada barang direct buy yang dilempar dari BuyerDetailActivity
        if (getIntent().hasExtra("EXTRA_DIRECT_BUY_ITEM")) {
            isDirectBuy = true;
            CartItem singleItem = (CartItem) getIntent().getSerializableExtra("EXTRA_DIRECT_BUY_ITEM");

            if (singleItem != null) {
                checkoutList.add(singleItem);

                // Hitung subtotal untuk satu barang ini
                int multiplier = 1;
                if ("Mingguan".equals(singleItem.getDurationType())) multiplier = 7;
                else if ("Bulanan".equals(singleItem.getDurationType())) multiplier = 30;
                subTotal = (long) singleItem.getPrice() * singleItem.getQuantity() * singleItem.getDurationValue() * multiplier;
            }

            adapter.notifyDataSetChanged();
            updatePriceUI();

        } else {
            // JIKA BUKAN DIRECT BUY: Tarik data dari Keranjang Firestore (Kode lama)
            isDirectBuy = false;
            db.collection("users").document(currentUserId).collection("cart")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            CartItem item = doc.toObject(CartItem.class);
                            if (item != null) {
                                checkoutList.add(item);

                                int multiplier = 1;
                                if ("Mingguan".equals(item.getDurationType())) multiplier = 7;
                                else if ("Bulanan".equals(item.getDurationType())) multiplier = 30;
                                subTotal += (long) item.getPrice() * item.getQuantity() * item.getDurationValue() * multiplier;
                            }
                        }
                        adapter.notifyDataSetChanged();
                        updatePriceUI();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show());
        }
    }

    private void updatePriceUI() {
        grandTotal = subTotal + ONGKOS_KIRIM;

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);

        binding.tvCheckoutSubtotal.setText(formatRupiah.format(subTotal));
        binding.tvCheckoutShipping.setText(formatRupiah.format(ONGKOS_KIRIM));
        binding.tvCheckoutFinalTotal.setText(formatRupiah.format(grandTotal));
    }

    // ==========================================
    // 2. MESIN INJEKSI PESANAN (WRITE BATCH)
    // ==========================================
    private void processOrder() {
        String address = binding.etDeliveryAddress.getText().toString().trim();

        // Validasi Alamat
        if (address.isEmpty()) {
            binding.etDeliveryAddress.setError("Alamat pengiriman wajib diisi!");
            binding.etDeliveryAddress.requestFocus();
            return;
        }

        if (checkoutList.isEmpty()) {
            Toast.makeText(this, "Pesanan kosong atau belum termuat.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tentukan Metode Pembayaran
        int selectedPaymentId = binding.rgPaymentMethod.getCheckedRadioButtonId();
        RadioButton selectedRb = findViewById(selectedPaymentId);
        String paymentMethod = selectedRb.getText().toString();

        // Siapkan Progress Dialog agar pembeli tidak klik 2 kali
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang memproses pesananmu...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Buat ID Pesanan Unik
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Ambil daftar ID penjual (agar nanti penjual bisa memfilter pesanan ini)
        List<String> sellerIds = new ArrayList<>();
        for (CartItem item : checkoutList) {
            if (!sellerIds.contains(item.getOwnerId())) {
                sellerIds.add(item.getOwnerId());
            }
        }

        // Kumpulkan data ke dalam "Koper Pesanan" (HashMap)
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("buyerId", currentUserId);
        orderData.put("sellerIds", sellerIds); // Daftar penjual yang terlibat
        orderData.put("items", checkoutList); // Seluruh isi keranjang dimasukkan ke pesanan
        orderData.put("deliveryAddress", address);
        orderData.put("paymentMethod", paymentMethod);
        orderData.put("subTotal", subTotal);
        orderData.put("shippingCost", ONGKOS_KIRIM);
        orderData.put("grandTotal", grandTotal);
        orderData.put("status", "Menunggu Konfirmasi"); // Status Awal
        orderData.put("orderDate", new Date());

        // Mulai Transaksi Batch
        WriteBatch batch = db.batch();

        // 1. Masukkan dokumen pesanan baru ke koleksi "orders"
        batch.set(db.collection("orders").document(orderId), orderData);

        batch.update(db.collection("users").document(currentUserId), "address", address);

        // 2. Hapus satu per satu item di keranjang pembeli
        if (!isDirectBuy) {
            for (CartItem item : checkoutList) {
                batch.delete(db.collection("users").document(currentUserId)
                        .collection("cart").document(item.getProductId()));
            }
        }

        // 3. Eksekusi Batch
        batch.commit().addOnSuccessListener(aVoid -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Hore! Pesanan Berhasil Dibuat \uD83C\uDF89", Toast.LENGTH_LONG).show();

            // Tutup halaman ini dan (opsional) kembali ke Beranda
            finish();
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Gagal membuat pesanan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    // ==========================================
    // SUNTIKAN BARU: Tarik Alamat Saat Halaman Dibuka
    // ==========================================
    private void loadUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.florist.model.User userProfile = documentSnapshot.toObject(com.example.florist.model.User.class);

                        if (userProfile != null && userProfile.getAddress() != null && !userProfile.getAddress().isEmpty()) {
                            binding.etDeliveryAddress.setText(userProfile.getAddress());
                        }
                    }
                });
    }
}