package com.example.florist.views.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.adapter.BuyerImageSliderAdapter;
import com.example.florist.databinding.ActivityBuyerDetailBinding;
import com.example.florist.databinding.DialogAddToCartBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Product;
import com.example.florist.viewmodels.BuyerDetailViewModel;
import com.example.florist.viewmodels.CartViewModel;
import com.example.florist.views.chat.ChatRoomActivity;
import com.example.florist.views.homepage.ShopProfileActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class BuyerDetailActivity extends AppCompatActivity {
    private ActivityBuyerDetailBinding binding;
    private BuyerDetailViewModel viewModel;
    private CartViewModel cartViewModel;
    private Product product;
    private boolean isFavorite = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            binding = ActivityBuyerDetailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            viewModel = new ViewModelProvider(this).get(BuyerDetailViewModel.class);
            cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
            product = (Product) getIntent().getSerializableExtra("EXTRA_PRODUCT");

            if (product != null) {
                setupObservers();
                showProductData();
                viewModel.fetchShopData(product.getOwnerId());
            } else {
                Toast.makeText(this, "Gagal memuat detail produk", Toast.LENGTH_SHORT).show();
                finish();
            }

            setupButtons();
            setupFadingHeader();
            setupFavoriteButton();

            cartViewModel.loadCartCount();
        }

    private void setupObservers() {
        cartViewModel.getCartBadgeCount().observe(this, this::updateCartBadgeUI);
        viewModel.getShopData().observe(this, shop -> {
            if (shop != null) {
                binding.tvShopName.setText(shop.getShopName());
                binding.tvLocation.setText("Kota " + shop.getShopCity());
                binding.etSearch.setHint("Cari + " + product.getName());

                Glide.with(this)
                        .load(shop.getShopImageUrl())
                        .placeholder(R.drawable.building)
                        .circleCrop()
                        .into(binding.imgShop);
            } else {
                binding.tvShopName.setText("Toko tidak ditemukan");
                binding.tvLocation.setText("-");
            }
        });

        viewModel.getAddToCartSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Berhasil ditambahakan di keranjang", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupFadingHeader() {
            binding.etSearch.setAlpha(0f);
            binding.etSearch.setEnabled(false);
            binding.headerLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            binding.svMainContent.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

                        float fadeDistance = 350f;

                        float percentage = scrollY / fadeDistance;
                        if (percentage > 1.0f) percentage = 1.0f;
                        if (percentage < 0.0f) percentage = 0.0f;

                        binding.etSearch.setAlpha(percentage);
                        binding.etSearch.setEnabled(percentage > 0.5f);

                        int alphaColor = (int) (percentage * 255);
                        int dynamicWhiteColor = android.graphics.Color.argb(alphaColor, 255, 255, 255);

                        binding.headerLayout.setBackgroundColor(dynamicWhiteColor);

                        if (percentage >= 1.0f) {
                            binding.headerLayout.setElevation(8f);
                        } else {
                            binding.headerLayout.setElevation(0f);
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
            if (product != null && product.getOwnerId() != null) {
                android.content.Intent intent = new android.content.Intent(this, ChatRoomActivity.class);
                intent.putExtra("EXTRA_TARGET_ID", product.getOwnerId());
                intent.putExtra("EXTRA_TARGET_NAME", binding.tvShopName.getText().toString());String targetImage = "";
                intent.putExtra("EXTRA_TARGET_IMAGE", targetImage);
                startActivity(intent);
            }
            Toast.makeText(this, "Membuka obrolan dengan penjual...", Toast.LENGTH_SHORT).show();
        });
        binding.btnCart.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
        });

        binding.btnVisitShop.setOnClickListener(v -> {
            if (product != null && product.getOwnerId() != null) {
                Intent intent = new Intent(this, ShopProfileActivity.class);
                intent.putExtra("EXTRA_SHOP_ID", product.getOwnerId());
                startActivity(intent);
            }
        });

        binding.tvShopName.setOnClickListener(v -> {
            if (product != null && product.getOwnerId() != null) {
                Intent intent = new Intent(this, ShopProfileActivity.class);
                intent.putExtra("EXTRA_SHOP_ID", product.getOwnerId());
                startActivity(intent);
            }
        });

        binding.btnAddToCart.setOnClickListener(v -> {
            showAddToCartDialog(false);
        });
        binding.btnDirectBuy.setOnClickListener(v -> {
            showAddToCartDialog(true);
        });
    }

    private void showProductData () {
        binding.tvName.setText(product.getName());
        binding.tvShopName.setText("Memuat...");

        String desc = product.getDescription();
        binding.tvDescription.setText(desc != null && !desc.isEmpty() ? desc : "Tidak ada deskripsi.");

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new java.util.Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
        binding.tvPrice.setText(formatRupiah.format(product.getPrice()));

        List<BuyerImageSliderAdapter.SliderItem> mediaItems = new ArrayList<>();
        if (product.getVideoUrls() != null && !product.getVideoUrls().isEmpty()) {
            for (String videoUrl : product.getVideoUrls()) {
                mediaItems.add(new BuyerImageSliderAdapter.SliderItem(videoUrl, BuyerImageSliderAdapter.SliderItem.TYPE_VIDEO));
            }
        }

        if (product.getGallery() != null && !product.getGallery().isEmpty()) {
            for (String imageUrl : product.getGallery()) {
                mediaItems.add(new BuyerImageSliderAdapter.SliderItem(imageUrl, BuyerImageSliderAdapter.SliderItem.TYPE_IMAGE));
            }
        } else {
            if (product.getImageUrl() != null) {
                mediaItems.add(new BuyerImageSliderAdapter.SliderItem(product.getImageUrl(), BuyerImageSliderAdapter.SliderItem.TYPE_IMAGE));
            }
        }

        BuyerImageSliderAdapter sliderAdapter = new BuyerImageSliderAdapter(this, mediaItems);
        binding.viewPagerImages.setAdapter(sliderAdapter);

        new TabLayoutMediator(
                binding.tabDots,
                binding.viewPagerImages,
                (tab, position) -> {
                }
        ).attach();
        }

    private void showAddToCartDialog(boolean isDirectBuy) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogAddToCartBinding dialogBinding = DialogAddToCartBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        viewModel.setProductData(product.getPrice(), product.getStock());

        dialogBinding.tvDialogName.setText(product.getName());
        dialogBinding.tvDialogStock.setText("Stok: " + product.getStock());

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

            // Warnai tombol yang aktif & Ubah Label Harga Dinamis
            if ("Harian".equals(type)) {
                dialogBinding.btnTypeHarian.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnTypeHarian.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.tvDialogPrice.setText(formatRupiah.format(product.getPrice()) + " /hari");
            } else if ("Mingguan".equals(type)) {
                dialogBinding.btnTypeMingguan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnTypeMingguan.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.tvDialogPrice.setText(formatRupiah.format(product.getPrice() * 7) + " /minggu");
            } else if ("Bulanan".equals(type)) {
                dialogBinding.btnTypeBulanan.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.olive_500));
                dialogBinding.btnTypeBulanan.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.tvDialogPrice.setText(formatRupiah.format(product.getPrice() * 30) + " /bulan");
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

        dialogBinding.btnSubmitCart.setText(isDirectBuy ? "Lanjut ke Pembayaran" : "Masukkan Keranjang");

        dialogBinding.btnSubmitCart.setOnClickListener(v -> {
            int finalQty = viewModel.getQuantity().getValue() != null ? viewModel.getQuantity().getValue() : 1;
            int finalDurValue = viewModel.getDurationValue().getValue() != null ? viewModel.getDurationValue().getValue() : 1;
            String finalDurType = viewModel.getDurationType().getValue() != null ? viewModel.getDurationType().getValue() : "Harian";
            String currentShopName = binding.tvShopName.getText().toString();

            if (isDirectBuy) {
                CartItem directBuyItem = viewModel.createCartItem(product, finalQty, finalDurType, finalDurValue, currentShopName);
                Intent intent = new Intent(this, CheckoutActivity.class);
                intent.putExtra("EXTRA_DIRECT_BUY_ITEM", directBuyItem);
                startActivity(intent);
                dialog.dismiss();
            } else {
                int[] startLocation = new int[2];
                dialogBinding.imgDialogProduct.getLocationOnScreen(startLocation);
                flyToCartAnimation(startLocation, dialogBinding.imgDialogProduct.getWidth(), dialogBinding.imgDialogProduct.getHeight());

                // DELEGASIKAN LOGIKA DATABASE KE VIEWMODEL!
                viewModel.addToCart(product, finalQty, finalDurType, finalDurValue, currentShopName);
                dialog.dismiss();
            }
        });
        dialog.show();
        }

    private void updateCartBadgeUI(int count) {
        if (count > 0) {
            binding.tvCartBadgeCount.setVisibility(View.VISIBLE);
            binding.tvCartBadgeCount.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            binding.tvCartBadgeCount.setVisibility(View.GONE);
        }
    }

    private void flyToCartAnimation(int[] startLocation, int imgWidth, int imgHeight) {
        ImageView flyingImage = new ImageView(this);
        String cartImageUrl = product.getImageUrl();
        if (product.getGallery() != null && !product.getGallery().isEmpty()){
            cartImageUrl = product.getGallery().get(0);
        }

        Glide.with(this).load(cartImageUrl).circleCrop().into(flyingImage);

        int[] rootLocation = new int[2];
        binding.getRoot().getLocationOnScreen(rootLocation);

        float startX = startLocation[0] - rootLocation[0];
        float startY = startLocation[1] - rootLocation[1];

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(imgWidth, imgHeight);
        flyingImage.setLayoutParams(params);
        flyingImage.setX(startX);
        flyingImage.setY(startY);
        flyingImage.setElevation(100f);

        binding.getRoot().addView(flyingImage);

        int[] targetLocation = new int[2];
        binding.btnCart.getLocationOnScreen(targetLocation);

        float targetX = targetLocation[0] - rootLocation[0] + (binding.btnCart.getWidth() / 2f) - (imgWidth / 2f);
        float targetY = targetLocation[1] - rootLocation[1] + (binding.btnCart.getHeight() / 2f) - (imgHeight / 2f);

        flyingImage.animate()
                .x(targetX)
                .y(targetY)
                .scaleX(0.1f)
                .scaleY(0.1f)
                .alpha(0.5f)
                .setDuration(1000)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    binding.getRoot().removeView(flyingImage);

                    binding.btnCart.animate()
                            .scaleX(1.3f).scaleY(1.3f)
                            .setDuration(150)
                            .withEndAction(() -> {
                                binding.btnCart.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                            }).start();
                }).start();
    }

}
