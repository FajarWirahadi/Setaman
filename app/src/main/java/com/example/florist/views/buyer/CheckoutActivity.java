package com.example.florist.views.buyer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
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
    private final long ONGKOS_KIRIM = 15000;
    private long grandTotal = 0;

    private DeliveryAddress finalDeliveryAddress = null;

    private final ActivityResultLauncher<Intent> addressLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            DeliveryAddress selected = (DeliveryAddress) result.getData().getSerializableExtra("SELECTED_ADDRESS");
                            if (selected != null) {
                                finalDeliveryAddress = selected;
                                updateAddressUI(selected);
                            }
                        }
                    });

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
        loadMainAddress();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        checkoutList = new ArrayList<>();
        adapter = new CheckoutAdapter(this, checkoutList);
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(adapter);

        binding.btnPlaceOrder.setOnClickListener(v -> processOrder());

        binding.layoutAddressSelection.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressSelectionActivity.class);
            addressLauncher.launch(intent);
        });
    }

    private void loadMainAddress() {
        db.collection("users").document(currentUserId).collection("addresses")
                .whereEqualTo("mainAddress", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DeliveryAddress mainAddress = queryDocumentSnapshots.getDocuments().get(0).toObject(DeliveryAddress.class);
                        if (mainAddress != null) {
                            finalDeliveryAddress = mainAddress;
                            updateAddressUI(mainAddress);
                        }
                    } else {
                        // Jika tidak ada alamat utama
                        binding.tvReceiverInfo.setText("Belum ada alamat");
                        binding.tvDeliveryAddress.setText("Klik di sini untuk memilih atau menambahkan alamat pengiriman.");
                    }
                });
    }

    private void updateAddressUI(DeliveryAddress address) {
        binding.tvReceiverInfo.setText(address.getReceiverName() + " (" + address.getPhoneNumber() + ")");
        binding.tvDeliveryAddress.setText(address.getFullAddress());
    }

    private void loadCheckoutData() {
        checkoutList.clear();
        subTotal = 0;

        if (getIntent().hasExtra("EXTRA_DIRECT_BUY_ITEM")) {
            isDirectBuy = true;
            CartItem singleItem = (CartItem) getIntent().getSerializableExtra("EXTRA_DIRECT_BUY_ITEM");

            if (singleItem != null) {
                checkoutList.add(singleItem);

                int multiplier = 1;
                if ("Mingguan".equals(singleItem.getDurationType())) multiplier = 7;
                else if ("Bulanan".equals(singleItem.getDurationType())) multiplier = 30;
                subTotal = (long) singleItem.getPrice() * singleItem.getQuantity() * singleItem.getDurationValue() * multiplier;
            }

            adapter.notifyDataSetChanged();
            updatePriceUI();

        } else {
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

    private void processOrder() {
        if (checkoutList.isEmpty()) {
            Toast.makeText(this, "Pesanan kosong atau belum termuat.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi Alamat Baru
        if (finalDeliveryAddress == null) {
            Toast.makeText(this, "Silakan pilih alamat pengirimanmu terlebih dahulu!", Toast.LENGTH_LONG).show();
            return;
        }

        int selectedPaymentId = binding.rgPaymentMethod.getCheckedRadioButtonId();
        RadioButton selectedRb = findViewById(selectedPaymentId);
        String paymentMethod = selectedRb.getText().toString();

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang memproses pesananmu...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        List<String> sellerIds = new ArrayList<>();
        for (CartItem item : checkoutList) {
            if (!sellerIds.contains(item.getOwnerId())) {
                sellerIds.add(item.getOwnerId());
            }
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("buyerId", currentUserId);
        orderData.put("sellerIds", sellerIds);
        orderData.put("items", checkoutList);
        orderData.put("paymentMethod", paymentMethod);
        orderData.put("subTotal", subTotal);
        orderData.put("shippingCost", ONGKOS_KIRIM);
        orderData.put("grandTotal", grandTotal);
        orderData.put("status", "Menunggu Konfirmasi");
        orderData.put("orderDate", new Date());

        orderData.put("deliveryAddress", finalDeliveryAddress.getFullAddress());
        orderData.put("receiverName", finalDeliveryAddress.getReceiverName());
        orderData.put("receiverPhone", finalDeliveryAddress.getPhoneNumber());

        WriteBatch batch = db.batch();

        batch.set(db.collection("orders").document(orderId), orderData);

        if (!isDirectBuy) {
            for (CartItem item : checkoutList) {
                batch.delete(db.collection("users").document(currentUserId)
                        .collection("cart").document(item.getProductId()));
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Hore! Pesanan Berhasil Dibuat \uD83C\uDF89", Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Gagal membuat pesanan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}