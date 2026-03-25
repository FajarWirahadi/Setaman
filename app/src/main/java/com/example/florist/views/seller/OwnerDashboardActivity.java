package com.example.florist.views.seller;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ActivityOwnerDashboardBinding;
import com.example.florist.viewmodels.OwnerDashboardViewModel;
import com.example.florist.views.seller.addproduct.AddProductActivity;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.security.acl.Owner;

public class OwnerDashboardActivity extends AppCompatActivity {

    private ActivityOwnerDashboardBinding binding;
    private OwnerDashboardViewModel viewModel;
    private String currentShopImageUrl = "";

    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri croppedUri = UCrop.getOutput(result.getData());
                    if (croppedUri != null) {
                        viewModel.uploadNewProfileImage(croppedUri);
                    }
                }
                else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                    // KONDISI 2: ERROR UCROP
                    Throwable cropError = UCrop.getError(result.getData());
                    Toast.makeText(this, "Crop error: " + (cropError != null ? cropError.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
                }
            }
    );
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String destinationFileName = "CROP_" + System.currentTimeMillis() + ".jpg";
                    Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));
                    UCrop.Options options = new UCrop.Options();
                    options.setCircleDimmedLayer(true);
                    options.setShowCropGrid(false);
                    // 2. MASUKKAN DUA URI TERSEBUT KE UCROP. BUKAN STRING! BUKAN IMAGEVIEW!
                    Intent uCropIntent = UCrop.of(uri, destinationUri)
                            .withAspectRatio(1, 1)
                            .withMaxResultSize(800, 800)
                            .withOptions(options)
                            .getIntent(this);

                    // 3. Panggil layar pemotong gambar
                    cropLauncher.launch(uCropIntent);
                }
            }
    );



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOwnerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(OwnerDashboardViewModel.class);

        setupToolbar();
        setupListeners();
        setupObservers();



        viewModel.loadDashboardData();
        viewModel.loadTotalProducts();
    }


    private void setupObservers() {
        viewModel.getShopData().observe(this, shop -> {
            if (shop != null) {
                binding.tvShopName.setText(shop.getShopName());
                binding.tvShopId.setText(shop.getShopId());

                binding.txtBelumDibayar.setText("-");
                binding.txtDalamProses.setText("-");
                binding.txtDalamPerawatan.setText("-");
                binding.txtDikirim.setText("-");

                if (shop.getShopImageUrl() != null && !shop.getShopImageUrl().isEmpty()) {

                    currentShopImageUrl = shop.getShopImageUrl();
                    Glide.with(this)
                            .load(shop.getShopImageUrl())
                            .placeholder(R.color.gray_100)
                            .error(R.drawable.building)
                            .circleCrop()
                            .into(binding.imgShopProfile);

                } else {
                    currentShopImageUrl = "";
                    binding.imgShopProfile.setImageResource(R.drawable.building);
                }
            }
        });

        viewModel.getTotalProducts().observe(this, count -> {
            if (count != null) {
                binding.tvTotalProducts.setText(String.valueOf(count));
            }
        });

        viewModel.getUpdateImageSuccess().observe(this, newImageUrl -> {
            if (newImageUrl != null) {
                Toast.makeText(this, "Foto profil toko berhasil diperharui", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                binding.loadingOverlay.setVisibility(View.VISIBLE);
            } else {
                binding.loadingOverlay.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            Toast.makeText(this, "Error: " + message, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        binding.btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboardActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        binding.btnVisitShop.setOnClickListener(v -> {
            Toast.makeText(this, "Fitur kunjungi toko (Coming Soon", Toast.LENGTH_SHORT).show();
        });

        binding.layoutTotalProducts.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboardActivity.this, MyProductActivity.class);
            startActivity(intent);
        });

        binding.imgShopProfile.setOnClickListener(v -> {
            showImagePreviewDialog();
        });
    }

    private void showImagePreviewDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_image_preview);

        // Buat agar lebarnya menyesuaikan layar
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Hilangkan background bawaan

        // 2. Hubungkan View yang ada di dalam XML Dialog
        android.widget.ImageView imgPreview = dialog.findViewById(R.id.imgPreview);
        android.widget.Button btnChangePhoto = dialog.findViewById(R.id.btnChangePhoto);

        // 3. Muat gambar saat ini ke dalam ImageView Dialog menggunakan Glide
        if (!currentShopImageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(currentShopImageUrl)
                    .placeholder(R.color.gray_100)
                    .circleCrop()
                    .into(imgPreview);
        } else {
            Glide.with(this).load(R.drawable.building).circleCrop().into(imgPreview);
        }

        // 4. Aksi saat tombol "Ganti Foto" ditekan di dalam Dialog
        btnChangePhoto.setOnClickListener(v -> {
            dialog.dismiss(); // Tutup dialognya dulu
            imagePickerLauncher.launch("image/*"); // Buka galeri!
        });

        // 5. Tampilkan Dialog ke layar
        dialog.show();
    }

    private void setupToolbar() {

    }
}