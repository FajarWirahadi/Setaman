package com.example.florist.views.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ActivityProductDetailBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Product;
import com.example.florist.views.buyer.CheckoutActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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

    // ==========================================
    // 1. SETUP LISTENERS BARU
    // ==========================================
    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnChat.setOnClickListener(v -> {
            Toast.makeText(this, "Membuka obrolan...", Toast.LENGTH_SHORT).show();
            // Nanti arahkan ke ChatRoomActivity
        });

        binding.btnAddToCart.setOnClickListener(v -> {
            showOrderDialog(false); // Mode "Masukkan Keranjang"
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            showOrderDialog(true);
        });
    }

    private void showOrderDialog(boolean isDirectBuy) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_to_cart, null);
        dialog.setContentView(view);

        // Binding Komponen UI di Bottom Sheet
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        ImageView imgProduct = view.findViewById(R.id.imgDialogProduct);
        TextView tvName = view.findViewById(R.id.tvDialogName);
        TextView tvPrice = view.findViewById(R.id.tvDialogPrice);
        TextView tvTotalPrice = view.findViewById(R.id.tvDialogTotalPrice);
        Button btnSubmit = view.findViewById(R.id.btnSubmitCart);

        com.google.android.material.button.MaterialButton btnMinQty = view.findViewById(R.id.btnMinQty);
        com.google.android.material.button.MaterialButton btnAddQty = view.findViewById(R.id.btnAddQty);
        TextView tvQtyValue = view.findViewById(R.id.tvQtyValue);

        com.google.android.material.button.MaterialButton btnMinDuration = view.findViewById(R.id.btnMinDuration);
        com.google.android.material.button.MaterialButton btnAddDuration = view.findViewById(R.id.btnAddDuration);
        TextView tvDurationValue = view.findViewById(R.id.tvDurationValue);

        Button btnHarian = view.findViewById(R.id.btnTypeHarian);
        Button btnMingguan = view.findViewById(R.id.btnTypeMingguan);
        Button btnBulanan = view.findViewById(R.id.btnTypeBulanan);

        tvName.setText(product.getName());

        String imageUrl = product.getImageUrl();
        if (product.getGallery() != null && !product.getGallery().isEmpty()) {
            imageUrl = product.getGallery().get(0);
        }
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imgProduct);
        }

        btnSubmit.setText(isDirectBuy ? "Sewa Sekarang" : "Masukkan Keranjang");

        final int[] qty = {1};
        final int[] durationMultiplier = {1};
        final double[] basePrice = {product.getPrice()};
        final String[] durationLabel = {product.getDuration() != null ? product.getDuration() : "Hari"};

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatter.setMaximumFractionDigits(0);

        Runnable updateUI = () -> {
            tvQtyValue.setText(String.valueOf(qty[0]));
            tvDurationValue.setText(String.valueOf(durationMultiplier[0]));

            if ("Hari".equals(durationLabel[0])) {
                tvPrice.setText(formatter.format(basePrice[0]) + " /hari");
            } else if ("Minggu".equals(durationLabel[0])) {
                tvPrice.setText(formatter.format(basePrice[0] * 7) + " /minggu");
            } else if ("Bulan".equals(durationLabel[0])) {
                tvPrice.setText(formatter.format(basePrice[0] * 30) + " /bulan");
            }

            double total = basePrice[0] * qty[0] * durationMultiplier[0];

            // Sesuaikan pengali paket untuk total
            if ("Minggu".equals(durationLabel[0])) total *= 7;
            if ("Bulan".equals(durationLabel[0])) total *= 30;

            tvTotalPrice.setText(formatter.format(total));
        };

        btnAddQty.setOnClickListener(v -> { qty[0]++; updateUI.run(); });
        btnMinQty.setOnClickListener(v -> { if (qty[0] > 1) { qty[0]--; updateUI.run(); } });

        btnAddDuration.setOnClickListener(v -> { durationMultiplier[0]++; updateUI.run(); });
        btnMinDuration.setOnClickListener(v -> { if (durationMultiplier[0] > 1) { durationMultiplier[0]--; updateUI.run(); } });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        View.OnClickListener typeListener = v -> {
            btnHarian.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            btnHarian.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            btnMingguan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            btnMingguan.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            btnBulanan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            btnBulanan.setTextColor(ContextCompat.getColor(this, R.color.gray_700));

            v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.olive_500)));
            ((Button)v).setTextColor(ContextCompat.getColor(this, R.color.white));

            if (v.getId() == R.id.btnTypeHarian) {
                durationLabel[0] = "Hari";
            } else if (v.getId() == R.id.btnTypeMingguan) {
                durationLabel[0] = "Minggu";
            } else if (v.getId() == R.id.btnTypeBulanan) {
                durationLabel[0] = "Bulan";
            }
            durationMultiplier[0] = 1;
            updateUI.run();
        };

        btnHarian.setOnClickListener(typeListener);
        btnMingguan.setOnClickListener(typeListener);
        btnBulanan.setOnClickListener(typeListener);

        btnSubmit.setOnClickListener(v -> {
            dialog.dismiss();

            CartItem item = new CartItem();
            item.setProductId(product.getProductId() != null ? product.getProductId() : "");
            item.setName(product.getName());

            String finalImageUrl = product.getImageUrl();
            if (product.getGallery() != null && !product.getGallery().isEmpty()) {
                finalImageUrl = product.getGallery().get(0);
            }
            item.setImageUrl(finalImageUrl);

            item.setPrice(product.getPrice());
            item.setQuantity(qty[0]);

            int durationInDays = durationMultiplier[0];
            if ("Minggu".equals(durationLabel[0])) durationInDays *= 7;
            if ("Bulan".equals(durationLabel[0])) durationInDays *= 30;

            item.setDurationValue(durationInDays);
            item.setDurationType(durationLabel[0]);
            item.setOwnerId(product.getOwnerId());

            if (isDirectBuy) {
                // LEMPAR KE CHECKOUT ACTIVITY
                Intent intent = new Intent(this, CheckoutActivity.class);
                intent.putExtra("EXTRA_DIRECT_BUY_ITEM", item);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Berhasil ditambahkan ke keranjang!", Toast.LENGTH_SHORT).show();
            }
        });

        // Tampilkan Dialog (Default Harian)
        btnHarian.performClick();
        dialog.show();
    }

    // ==========================================
    // 3. FUNGSI BAWAAN ANDA (TIDAK BERUBAH)
    // ==========================================
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
        binding.tvDescription.setText(product.getDescription());

        NumberFormat formatRp = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRp.setMaximumFractionDigits(0);
        binding.tvProductPrice.setText(formatRp.format(product.getPrice()) + "/hari");

        binding.tvRating.setText(" 4.5/5 (20) | ");
        binding.tvSold.setText("20 Tersewa");
    }
}