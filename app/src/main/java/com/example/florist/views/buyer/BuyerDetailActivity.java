package com.example.florist.views.buyer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.adapter.BuyerImageSliderAdapter;
import com.example.florist.databinding.ActivityBuyerDetailBinding;
import com.example.florist.databinding.DialogAddToCartBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Product;
import com.example.florist.model.Shop;
import com.example.florist.viewmodels.BuyerDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BuyerDetailActivity extends AppCompatActivity {
    private ActivityBuyerDetailBinding binding;
    private BuyerDetailViewModel viewModel;
    private Product product;
    private boolean isFavorite = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            binding = ActivityBuyerDetailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            viewModel = new ViewModelProvider(this).get(BuyerDetailViewModel.class);

            product = (Product) getIntent().getSerializableExtra("EXTRA_PRODUCT");

            if (product != null) {
                showProductData();
            } else {
                Toast.makeText(this, "Gagal memuat detail produk", Toast.LENGTH_SHORT).show();
                finish();
            }

            setupButtons();
            setupFadingHeader();
            setupFavoriteButton();
        }

    private void fetchShopData(String ownerId) {
        FirebaseFirestore.getInstance().collection("shops")
                .document(ownerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Shop shop = documentSnapshot.toObject(Shop.class);
                        if (shop != null) {
                            binding.tvShopName.setText(shop.getShopName());
                            binding.tvLocation.setText("Kota " + shop.getShopCity());
                            binding.etSearch.setHint("Cari " + product.getName());

                            Glide.with(this)
                                    .load(shop.getShopImageUrl())
                                    .placeholder(R.drawable.building)
                                    .circleCrop()
                                    .into(binding.imgShop);
                        }
                    } else {
                        binding.tvShopName.setText("Toko tidak ditemukan");
                        binding.tvLocation.setText("-");
                    }
                })
                .addOnFailureListener(e -> {
                    binding.tvShopName.setText("Gagal memuat toko");
                });
    }

    private void setupFadingHeader() {
            // 1. Kondisi Awal Saat Halaman Dibuka
            binding.etSearch.setAlpha(0f); // Sembunyikan EditText
            binding.etSearch.setEnabled(false); // Jangan bisa diklik saat hilang
            binding.headerLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            // 2. Pasang Sensor Scroll pada NestedScrollView
            binding.svMainContent.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

                        // Jarak guliran di mana header akan 100% putih (misal 350 pixel)
                        // Sesuaikan angka ini dengan tinggi gambar bannermu!
                        float fadeDistance = 350f;

                        // Hitung persentase scroll (0.0 sampai 1.0)
                        float percentage = scrollY / fadeDistance;
                        if (percentage > 1.0f) percentage = 1.0f;
                        if (percentage < 0.0f) percentage = 0.0f;

                        // --- A. EFEK MUNCULNYA EDITTEXT ---
                        binding.etSearch.setAlpha(percentage);
                        // Aktifkan klik jika sudah setengah muncul
                        binding.etSearch.setEnabled(percentage > 0.5f);

                        // --- B. EFEK PERUBAHAN WARNA BACKGROUND HEADER ---
                        // 255 adalah nilai Solid (tidak tembus pandang). Kita kalikan dengan persentase.
                        int alphaColor = (int) (percentage * 255);
                        // Buat warna putih dengan tingkat transparansi dinamis
                        int dynamicWhiteColor = android.graphics.Color.argb(alphaColor, 255, 255, 255);

                        binding.headerLayout.setBackgroundColor(dynamicWhiteColor);

                        // --- C. (OPSIONAL) EFEK BAYANGAN / ELEVASI ---
                        if (percentage >= 1.0f) {
                            binding.headerLayout.setElevation(8f); // Beri bayangan saat solid
                        } else {
                            binding.headerLayout.setElevation(0f); // Hilangkan bayangan saat transparan
                        }
                    });
        }

    private void setupFavoriteButton() {
        binding.btnFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteUI();

            String message = isFavorite ?
                    product.getName() + " Ditambahkan ke favorit!" :
                    product.getName() + " Dihapus dari favorit!";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFavoriteUI() {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite);
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.olive_500));
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.gray_800));
        }
    }


    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnChat.setOnClickListener(v -> {
            Toast.makeText(this, "Membuka obrolan dengan penjual...", Toast.LENGTH_SHORT).show();
        });
        binding.btnMyCart.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
        });

        binding.btnAddToCart.setOnClickListener(v -> {
            showAddToCartDialog();
        });
    }

    private void showProductData () {
            binding.tvName.setText(product.getName());
            binding.tvShopName.setText(product.getOwnerId());

            String desc = product.getDescription();
            binding.tvDescription.setText(desc != null && !desc.isEmpty() ? desc : "Tidak ada deskripsi.");

            NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new java.util.Locale("in", "ID"));
            formatRupiah.setMaximumFractionDigits(0);
            binding.tvPrice.setText(formatRupiah.format(product.getPrice()));

           List<BuyerImageSliderAdapter.SliderItem> mediaItems = new ArrayList<>();
           if (product.getVideoUrls() != null && !product.getVideoUrls().isEmpty()) {
               for (String videoUrl: product.getVideoUrls()) {
                   mediaItems.add(new BuyerImageSliderAdapter.SliderItem(videoUrl, BuyerImageSliderAdapter.SliderItem.TYPE_VIDEO));
               }
           }

           if (product.getGallery() != null && !product.getGallery().isEmpty()) {
               for (String imageUrl : product.getGallery()) {
                   mediaItems.add(new BuyerImageSliderAdapter.SliderItem(imageUrl, BuyerImageSliderAdapter.SliderItem.TYPE_IMAGE));
               }
           } else {
               if (product.getImageUrl()!=null) {
                   mediaItems.add(new BuyerImageSliderAdapter.SliderItem(product.getImageUrl(), BuyerImageSliderAdapter.SliderItem.TYPE_IMAGE));
               }
           }

           BuyerImageSliderAdapter sliderAdapter = new BuyerImageSliderAdapter(this, mediaItems);
           binding.viewPagerImages.setAdapter(sliderAdapter);

            new TabLayoutMediator(
                    binding.tabDots,
                    binding.viewPagerImages,
                    (tab, position) -> {}
            ).attach();

            if (product.getOwnerId() != null && !product.getOwnerId().isEmpty()) {
                fetchShopData(product.getOwnerId());
            }
        }

    private void showAddToCartDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        DialogAddToCartBinding dialogBinding = DialogAddToCartBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());


        viewModel.setProductData(product.getPrice(), product.getStock());

        dialogBinding.tvDialogName.setText(product.getName());
        dialogBinding.tvDialogStock.setText("Stok: " + product.getStock());
        dialogBinding.tvDialogTotalPrice.setText("Rp " + String.valueOf(product.getPrice()));

        NumberFormat formatRupiah = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
        dialogBinding.tvDialogPrice.setText(formatRupiah.format(product.getPrice()) + " /hari");

        String imageUrl = product.getImageUrl();
        if (product.getGallery() != null && !product.getGallery().isEmpty()) {
            imageUrl = product.getGallery().get(0);
        }
        Glide.with(this).load(imageUrl).into(dialogBinding.imgDialogProduct);

        viewModel.getQuantity().observe(this, qty -> dialogBinding.tvQtyValue.setText(String.valueOf(qty)));
        viewModel.getDurationValue().observe(this, dur -> dialogBinding.tvDurationValue.setText(String.valueOf(dur)));
        viewModel.getTotalPrice().observe(this, total -> dialogBinding.tvDialogTotalPrice.setText(formatRupiah.format(total)));

        viewModel.getDurationType().observe(this, type -> {
            dialogBinding.btnTypeHarian.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            dialogBinding.btnTypeHarian.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            dialogBinding.btnTypeMingguan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            dialogBinding.btnTypeMingguan.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            dialogBinding.btnTypeBulanan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
            dialogBinding.btnTypeBulanan.setTextColor(ContextCompat.getColor(this, R.color.gray_700));

            // Warnai yang aktif jadi Olive
            if (type.equals("Harian")) {
                dialogBinding.btnTypeHarian.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnTypeHarian.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else if (type.equals("Mingguan")) {
                dialogBinding.btnTypeMingguan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnTypeMingguan.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else if (type.equals("Bulanan")) {
                dialogBinding.btnTypeBulanan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnTypeBulanan.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        });

        dialogBinding.btnMinQty.setOnClickListener(v -> viewModel.decrementQuantity());
        dialogBinding.btnAddQty.setOnClickListener(v -> viewModel.incrementQuantity());

        dialogBinding.btnMinDuration.setOnClickListener(v -> viewModel.decrementDuration());
        dialogBinding.btnAddDuration.setOnClickListener(v -> viewModel.incrementDuration());

        dialogBinding.btnTypeHarian.setOnClickListener(v -> viewModel.setDurationType("Harian", 1));
        dialogBinding.btnTypeMingguan.setOnClickListener(v -> viewModel.setDurationType("Mingguan", 7));
        dialogBinding.btnTypeBulanan.setOnClickListener(v -> viewModel.setDurationType("Bulanan", 30));

        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSubmitCart.setOnClickListener(v -> {
            int[] startLocation = new int[2];
            dialogBinding.imgDialogProduct.getLocationOnScreen(startLocation);
            int imgWidth = dialogBinding.imgDialogProduct.getWidth();
            int imgHeight = dialogBinding.imgDialogProduct.getHeight();
            flyToCartAnimation(startLocation, imgWidth, imgHeight);
            dialog.dismiss();
            int finalQty = viewModel.getQuantity().getValue() != null ? viewModel.getQuantity().getValue() : 1;
            int finalDurValue = viewModel.getDurationValue().getValue() != null ? viewModel.getDurationValue().getValue() : 1;
            String finalDurType = viewModel.getDurationType().getValue() != null ? viewModel.getDurationType().getValue() : "Harian";

            executeAddToCartToFirestore(finalQty, finalDurType, finalDurValue);
        });
        dialog.show();
        }

    private void executeAddToCartToFirestore(int selectedQty, String selectedDurType, int selectedDurValue) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Silahkan login terlebih dahulu!", Toast.LENGTH_SHORT).show();
            return;
        }
        String buyerId = currentUser.getUid();
        String cartImageUrl = product.getImageUrl();
        if (product.getGallery() != null && !product.getGallery().isEmpty()){
            cartImageUrl = product.getGallery().get(0);
        }

        CartItem newCartItem = new CartItem(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                cartImageUrl,
                product.getOwnerId(),
                selectedQty,
                selectedDurType,
                selectedDurValue,
                new java.util.Date()
        );

        FirebaseFirestore.getInstance()
                .collection("users").document(buyerId)
                .collection("cart").document(product.getProductId())
                .set(newCartItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Berhasil ditambahkan ke keranjang", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menambahkan " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    private void flyToCartAnimation(int[] startLocation, int imgWidth, int imgHeight) {
        android.widget.ImageView flyingImage = new android.widget.ImageView(this);
        String cartImageUrl = product.getImageUrl();
        if (product.getGallery() != null && !product.getGallery().isEmpty()){
            cartImageUrl = product.getGallery().get(0);
        }

        Glide.with(this).load(cartImageUrl).circleCrop().into(flyingImage);

        int[] rootLocation = new int[2];
        binding.getRoot().getLocationOnScreen(rootLocation);

        float startX = startLocation[0] - rootLocation[0];
        float startY = startLocation[1] - rootLocation[1];

        android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(imgWidth, imgHeight);
        flyingImage.setLayoutParams(params);
        flyingImage.setX(startX);
        flyingImage.setY(startY);
        flyingImage.setElevation(100f);

        binding.getRoot().addView(flyingImage);

        int[] targetLocation = new int[2];
        binding.btnMyCart.getLocationOnScreen(targetLocation);

        float targetX = targetLocation[0] - rootLocation[0] + (binding.btnMyCart.getWidth() / 2f) - (imgWidth / 2f);
        float targetY = targetLocation[1] - rootLocation[1] + (binding.btnMyCart.getHeight() / 2f) - (imgHeight / 2f);

        flyingImage.animate()
                .x(targetX)
                .y(targetY)
                .scaleX(0.1f)
                .scaleY(0.1f)
                .alpha(0.5f)
                .setDuration(1000)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    binding.getRoot().removeView(flyingImage);

                    binding.btnMyCart.animate()
                            .scaleX(1.3f).scaleY(1.3f)
                            .setDuration(150)
                            .withEndAction(() -> {
                                binding.btnMyCart.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                            }).start();
                }).start();
    }

}
