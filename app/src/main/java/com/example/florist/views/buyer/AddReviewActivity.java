package com.example.florist.views.buyer;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ActivityAddReviewBinding;
import com.example.florist.viewmodels.ReviewViewModel;

public class AddReviewActivity extends AppCompatActivity {

    private ActivityAddReviewBinding binding;
    private ReviewViewModel reviewViewModel;

    private String orderId;
    private String productId;
    private String productName;
    private String productImageUrl;
    private Uri selectedImageUri = null;

    private final androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.llMediaPlaceholder.setVisibility(android.view.View.GONE);
                    binding.imgReviewPreview.setVisibility(android.view.View.VISIBLE);

                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(binding.imgReviewPreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        reviewViewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        orderId = getIntent().getStringExtra("EXTRA_ORDER_ID");
        productId = getIntent().getStringExtra("EXTRA_PRODUCT_ID");
        productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
        productImageUrl = getIntent().getStringExtra("EXTRA_PRODUCT_IMAGE");

        if (orderId == null || productId == null) {
            Toast.makeText(this, "Data pesanan tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupListeners();
        setupObservers();
    }

    private void setupUI() {
        binding.tvProductName.setText(productName);

        Glide.with(this)
                .load(productImageUrl)
                .placeholder(R.drawable.rounded_gray_layout)
                .centerCrop()
                .into(binding.imgProductThumb);

        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        binding.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            String desc = "";
            if (rating <= 1.0f) desc = "Sangat Buruk";
            else if (rating <= 2.0f) desc = "Buruk";
            else if (rating <= 3.0f) desc = "Cukup";
            else if (rating <= 4.0f) desc = "Baik";
            else desc = "Sangat Puas";
        });

        TextWatcher commonTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Hitung total panjang teks dari ketiga EditText
                int totalLength = binding.etReviewKondisi.length() +
                        binding.etReviewPelayanan.length() +
                        binding.etReviewPengiriman.length();

                binding.tvCharCounter.setText(totalLength + " karakter");

                // Opsional: Ubah warna jika mencapai minimal 50 karakter
                if (totalLength >= 50) {
                    binding.tvCharCounter.setTextColor(getResources().getColor(R.color.olive_500));
                } else {
                    binding.tvCharCounter.setTextColor(getResources().getColor(R.color.gray_500));
                }
            }
        };

        binding.etReviewKondisi.addTextChangedListener(commonTextWatcher);
        binding.etReviewPelayanan.addTextChangedListener(commonTextWatcher);
        binding.etReviewPengiriman.addTextChangedListener(commonTextWatcher);

        binding.layoutAddMedia.setOnClickListener(v -> {
            pickMedia.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.btnSubmitReview.setOnClickListener(v -> submitReviewData());

    }
    private void submitReviewData() {
        String kondisi = binding.etReviewKondisi.getText().toString().trim();
        String pelayanan = binding.etReviewPelayanan.getText().toString().trim();
        String pengiriman = binding.etReviewPengiriman.getText().toString().trim();
        float rating = binding.ratingBar.getRating();

        if (kondisi.isEmpty() && pelayanan.isEmpty() && pengiriman.isEmpty()) {
            Toast.makeText(this, "Silakan isi minimal satu kolom ulasan", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalComment = "Kondisi: " + (kondisi.isEmpty() ? "-" : kondisi) + "\n" +
                "Pelayanan: " + (pelayanan.isEmpty() ? "-" : pelayanan) + "\n" +
                "Pengiriman: " + (pengiriman.isEmpty() ? "-" : pengiriman);

        reviewViewModel.submitReview(orderId, productId, rating, finalComment, selectedImageUri);
    }

    private void setupObservers() {
        reviewViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.btnSubmitReview.setEnabled(false);
                binding.btnSubmitReview.setText("Mengirim...");
                binding.btnSubmitReview.setAlpha(0.7f);
            } else {
                binding.btnSubmitReview.setEnabled(true);
                binding.btnSubmitReview.setText("Kirim Ulasan");
                binding.btnSubmitReview.setAlpha(1.0f);
            }
        });

        reviewViewModel.getIsSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                Toast.makeText(this, "Terima kasih! Ulasanmu berhasil disimpan.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        reviewViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}