package com.example.florist.views.seller;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ActivityOwnerDashboardBinding;
import com.example.florist.viewmodels.OwnerDashboardViewModel;
import com.example.florist.views.chat.InboxActivity;
import com.example.florist.views.homepage.ShopProfileActivity;
import com.example.florist.views.seller.addproduct.AddProductActivity;
import com.yalantis.ucrop.UCrop;

import java.io.File;

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

                    Intent uCropIntent = UCrop.of(uri, destinationUri)
                            .withAspectRatio(1, 1)
                            .withMaxResultSize(800, 800)
                            .withOptions(options)
                            .getIntent(this);

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

    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.fetchMaintenanceAlerts();
        viewModel.loadDashboardData();
        viewModel.loadTotalProducts();
        viewModel.loadSellerOrderCounts();
    }

    private void setupObservers() {
        viewModel.getShopData().observe(this, shop -> {
            if (shop != null) {
                binding.tvShopName.setText(shop.getShopName());
                binding.tvShopCity.setText("Kota " + shop.getShopCity());


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

        viewModel.getCountUnpaid().observe(this, count -> {
            if (count > 0) {
                binding.badgeUnpaid.setVisibility(View.VISIBLE);
                binding.badgeUnpaid.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.badgeUnpaid.setVisibility(View.GONE);
            }
        });

        viewModel.getCountProcessing().observe(this, count -> {
            if (count > 0) {
                binding.badgeProcessing.setText(String.valueOf(count));
                binding.badgeProcessing.setVisibility(View.VISIBLE);
            } else {
                binding.badgeProcessing.setVisibility(View.GONE);
            }
        });

        viewModel.getCountShipped().observe(this, count -> {
            if (count > 0) {
                binding.badgeShipped.setText(String.valueOf(count));
                binding.badgeShipped.setVisibility(View.VISIBLE);
            } else {
                binding.badgeShipped.setVisibility(View.GONE);
            }
        });

//        viewModel.getCountMaintenance().observe(this, count -> {
//            if (count != null && count > 0) {
//                binding.tvMaintenanceAlert.setText("Ada " + count + " Tanaman menunggu perawatan!");
//                binding.tvMaintenanceAlert.setTextColor(getResources().getColor(R.color.text_error));
//            } else {
//                binding.tvMaintenanceAlert.setText("Tidak ada jadwal perawatan saat ini");
//                binding.tvMaintenanceAlert.setTextColor(getResources().getColor(R.color.text_success));
//            }
//        });

        viewModel.getCountUrgentComplaint().observe(this, urgentCount -> {
            Integer normalCount = viewModel.getCountComplaint().getValue();
            updateComplaintAlertUI(normalCount != null ? normalCount : 0, urgentCount != null ? urgentCount : 0);
        });

        // Amati Komplain Biasa
        viewModel.getCountComplaint().observe(this, normalCount -> {
            Integer urgentCount = viewModel.getCountUrgentComplaint().getValue();
            updateComplaintAlertUI(normalCount != null ? normalCount : 0, urgentCount != null ? urgentCount : 0);
        });

        viewModel.getTodayMaintenanceCount().observe(this, todayCount-> {
            Integer overdueCount = viewModel.getOverdueMaintenanceCount().getValue();
            updateMaintenanceAlertUI(todayCount, overdueCount != null? overdueCount : 0);
        });

        viewModel.getOverdueMaintenanceCount().observe(this, overdueCount -> {
            Integer todayCount = viewModel.getTodayMaintenanceCount().getValue();
            updateMaintenanceAlertUI(todayCount != null ? todayCount : 0, overdueCount);
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

    private void updateMaintenanceAlertUI(Integer todayCount, int overdueCount) {
        if (overdueCount > 0) {
            binding.layoutMaintenanceAlert.setBackgroundTintList(
                    ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.red_100)));

            binding.tvMaintenanceAlert.setText("Ada " + overdueCount + " tanaman yang TERLAMBAT dirawat!");
            binding.tvMaintenanceAlert.setTextColor(ContextCompat.getColor(this, R.color.text_error));

        } else if (todayCount > 0) {
            binding.layoutMaintenanceAlert.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_100)));

            binding.tvMaintenanceAlert.setText("Ada " + todayCount + " tanaman yang perlu dirawat hari ini.");
            binding.tvMaintenanceAlert.setTextColor(ContextCompat.getColor(this, R.color.green_600));

        } else {
            binding.layoutMaintenanceAlert.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray_100)));

            binding.tvMaintenanceAlert.setText("Tidak ada jadwal perawatan hari ini.");
            binding.tvMaintenanceAlert.setTextColor(ContextCompat.getColor(this, R.color.gray_600));
        }
    }

    private void updateComplaintAlertUI(int normalCount, int urgentCount) {
        if (urgentCount > 0) {
            // UI GENTING (Merah Gelap / Peringatan Keras)
            binding.tvComplaintTitle.setTextColor(ContextCompat.getColor(this, R.color.red_700));
            binding.tvComplaintCountText.setText("URGENT: " + urgentCount + " komplain butuh KUNJUNGAN FISIK!");
            binding.tvComplaintCountText.setTextColor(ContextCompat.getColor(this, R.color.red_700));

            binding.cvComplaintAlert.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red_100));
            binding.cvComplaintAlert.setStrokeColor(ContextCompat.getColor(this, R.color.red_700));
            binding.cvComplaintAlert.setStrokeWidth(5); // Border ditebalkan sebagai peringatan
            binding.icComplaintDash.setColorFilter(ContextCompat.getColor(this, R.color.red_700));

        } else if (normalCount > 0) {
            // UI Komplain Normal (Kuning/Oranye atau Merah Standar)
            binding.tvComplaintTitle.setTextColor(ContextCompat.getColor(this, R.color.yellow_600));
            binding.tvComplaintCountText.setText("Terdapat " + normalCount + " komplain menunggu tanggapan");
            binding.tvComplaintCountText.setTextColor(ContextCompat.getColor(this, R.color.yellow_600));

            binding.cvComplaintAlert.setCardBackgroundColor(ContextCompat.getColor(this, R.color.yellow_50));
            binding.cvComplaintAlert.setStrokeColor(ContextCompat.getColor(this, R.color.yellow_200));
            binding.cvComplaintAlert.setStrokeWidth(0);
            binding.icComplaintDash.setColorFilter(ContextCompat.getColor(this, R.color.yellow_500));

        } else {
            // UI Aman (Hijau)
            binding.tvComplaintTitle.setTextColor(ContextCompat.getColor(this, R.color.green_700));
            binding.tvComplaintCountText.setText("Tidak ada komplain saat ini");
            binding.tvComplaintCountText.setTextColor(ContextCompat.getColor(this, R.color.green_700));

            binding.cvComplaintAlert.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green_50));
            binding.cvComplaintAlert.setStrokeColor(ContextCompat.getColor(this, R.color.green_200));
            binding.cvComplaintAlert.setStrokeWidth(0);
            binding.icComplaintDash.setColorFilter(ContextCompat.getColor(this, R.color.green_500));
        }
    }

    private void setupListeners() {
        binding.myToolbar.btnInbox.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboardActivity.this, InboxActivity.class);
            startActivity(intent);
        });
        binding.btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboardActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        binding.btnVisitShop.setOnClickListener(v -> {
            if (viewModel.getShopData().getValue() != null) {
                String myShopId = viewModel.getShopData().getValue().getShopId();

                Intent intent = new Intent(this, ShopProfileActivity.class);
                intent.putExtra("EXTRA_SHOP_ID",myShopId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Data toko sedang dimuat, coba sesaat lagi.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.layoutTotalProducts.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboardActivity.this, MyProductActivity.class);
            startActivity(intent);
        });

        binding.tvHistory.setOnClickListener(v -> openSellerAtTab(3));

        binding.menuUnpaid.setOnClickListener(v -> openSellerAtTab(0));

        binding.menuProcessing.setOnClickListener(v -> openSellerAtTab(1));

        binding.menuShipped.setOnClickListener(v -> openSellerAtTab(2));

        binding.imgShopProfile.setOnClickListener(v -> {
            showImagePreviewDialog();
        });

        binding.cvMaintenanceShcedule.setOnClickListener(v -> {
            Toast.makeText(this, "Membuka jadwal perawatan", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MaintenanceScheduleActivity.class);
            startActivity(intent);
        });

        binding.cvComplaintAlert.setOnClickListener(v -> {
            Toast.makeText(this, "Membuka komplain dari pelanggan", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SellerComplaintListActivity.class);
            startActivity(intent);
        });
    }

    private void openSellerAtTab(int tabIndex) {
        Intent intent = new Intent(OwnerDashboardActivity.this, SellerOrderActivity.class);
        intent.putExtra("TAB_INDEX", tabIndex);
        startActivity(intent);
    }

    private void showImagePreviewDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_image_preview);

        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.ImageView imgPreview = dialog.findViewById(R.id.imgPreview);
        android.widget.Button btnChangePhoto = dialog.findViewById(R.id.btnChangePhoto);

        if (!currentShopImageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(currentShopImageUrl)
                    .placeholder(R.color.gray_100)
                    .circleCrop()
                    .into(imgPreview);
        } else {
            Glide.with(this).load(R.drawable.building).circleCrop().into(imgPreview);
        }

        btnChangePhoto.setOnClickListener(v -> {
            dialog.dismiss();
            imagePickerLauncher.launch("image/*");
        });

        dialog.show();
    }

    private void setupToolbar() {
        binding.myToolbar.btnBack.setOnClickListener(v -> onBackPressed());
    }
}