package com.example.florist.views.buyer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.adapter.BuyerImageSliderAdapter;
import com.example.florist.databinding.ActivityBuyerDetailBinding;
import com.example.florist.model.Product;
import com.example.florist.model.Shop;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class BuyerDetailActivity extends AppCompatActivity {
    private ActivityBuyerDetailBinding binding;
    private Product product;
    private boolean isFavorite = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            binding = ActivityBuyerDetailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

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

        binding.btnAddToCart.setOnClickListener(v -> {
            // NANTI KITA KERJAKAN: Logika menyimpan ke database keranjang!
            Toast.makeText(this, product.getName() + " berhasil dimasukkan ke keranjang!", Toast.LENGTH_SHORT).show();
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

    }
