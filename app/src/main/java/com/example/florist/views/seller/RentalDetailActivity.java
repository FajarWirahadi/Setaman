package com.example.florist.views.seller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.adapter.BuyerMaintenanceAdapter;
import com.example.florist.adapter.ChatResolutionAdapter;
import com.example.florist.adapter.ComplaintTimelineAdapter;
import com.example.florist.databinding.ActivityRentalDetailBinding;
import com.example.florist.databinding.DialogBuyerComplaintBinding;
import com.example.florist.model.Complaint;
import com.example.florist.utils.NetworkUtils;
import com.example.florist.viewmodels.BuyerMaintenanceViewModel;
import com.example.florist.viewmodels.ComplaintViewModel;
import com.example.florist.viewmodels.RentalDetailViewModel;
import com.example.florist.views.chat.ChatRoomActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RentalDetailActivity extends AppCompatActivity {

    private ActivityRentalDetailBinding binding;
    private String role;

    private String rentalId, orderId, storeName, receiverName;
    private String buyerId, sellerId;
    private BuyerMaintenanceViewModel buyerViewModel;
    private ComplaintViewModel buyerComplaintViewModel;
    private RentalDetailViewModel sellerViewModel;

    private BuyerMaintenanceAdapter timelineAdapter;
    private ComplaintTimelineAdapter complaintAdapter;
    private ChatResolutionAdapter chatAdapter;

    private String activeComplaintId = null;
    private BottomSheetDialog complaintDialog;
    private DialogBuyerComplaintBinding complaintBinding;
    private Uri complaintImageUri = null;
    private Uri cameraUri;

    // ==========================================
    // LAUNCHER KAMERA & GALERI
    // ==========================================
    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
        if (isSuccess && cameraUri != null) {
            complaintImageUri = cameraUri;
            if (complaintBinding != null) Glide.with(this).load(complaintImageUri).into(complaintBinding.imgComplaintPreview);
            checkComplaintValidation();
        }
    });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickComplaintMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            complaintImageUri = uri;
            if (complaintBinding != null) Glide.with(this).load(uri).into(complaintBinding.imgComplaintPreview);
            checkComplaintValidation();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRentalDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buyerComplaintViewModel = new ViewModelProvider(this).get(ComplaintViewModel.class);

        role = getIntent().getStringExtra("ROLE");
        if (role == null) role = "BUYER";

        rentalId = getIntent().getStringExtra("RENTAL_ID");
        orderId = getIntent().getStringExtra("ORDER_ID");
        storeName = getIntent().getStringExtra("STORE_NAME");
        if (storeName == null) storeName = "Toko Florist";

        if (rentalId == null || orderId == null) {
            Toast.makeText(this, "Data pesanan tidak lengkap.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupCommonUI();
        setupChatButton();

        if ("BUYER".equals(role)) {
            setupBuyerLogic();
        } else if ("SELLER".equals(role)) {
            setupSellerLogic();
        }

        handleAutoScroll(getIntent());
    }

    // ==========================================
    // SETUP UI BERSAMA (COMMON UI)
    // ==========================================
    private void setupCommonUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (Math.abs(verticalOffset) - appBarLayout.getTotalScrollRange() == 0) {
                binding.collapsingToolbar.setTitle("Riwayat Perawatan");
                if (binding.toolbar.getNavigationIcon() != null) binding.toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
            } else {
                binding.collapsingToolbar.setTitle("");
                if (binding.toolbar.getNavigationIcon() != null) binding.toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.main_color));
            }
        });

        complaintAdapter = new ComplaintTimelineAdapter();
        complaintAdapter.setStoreName(storeName);
        complaintAdapter.setQuoteListener(complaint -> {
            String dateStr = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(complaint.getCreatedAt().toDate());
            String title = "Komplain (" + dateStr + "): " + complaint.getReason();

            openChatRoomWithQuote(title, complaint.getDescription(), complaint.getEvidenceImageUrl(), complaint.getComplaintId(), "COMPLAINT");
        });
        binding.rvComplaintTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaintTimeline.setAdapter(complaintAdapter);

        timelineAdapter = new BuyerMaintenanceAdapter(log -> {
            String dateStr = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(log.getCreatedAt().toDate());
            String title = "Laporan Perawatan (" + dateStr + ")";

            openChatRoomWithQuote(title, log.getDescription(), log.getImageUrl(), log.getLogId(), "MAINTENANCE");
        });

        timelineAdapter.setSellerMode("SELLER".equals(role));
        timelineAdapter.setStoreName(storeName);
        binding.rvMaintenanceTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMaintenanceTimeline.setAdapter(timelineAdapter);
    }

    // ==========================================
    // LOGIKA TOMBOL CHAT PINTAR (BISA KOMPLAIN / CHAT UMUM)
    // ==========================================
    private void setupChatButton() {
        binding.btnSendChat.setOnClickListener(v -> {
            String text = binding.etChatMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            buyerComplaintViewModel.sendChatMessage(
                    rentalId,
                    role.equals("SELLER") ? "Penjual" : "Pembeli",
                    role.equals("SELLER") ? storeName : receiverName,
                    "",
                    text
            );

            binding.etChatMessage.setText("");

        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAutoScroll(intent);
    }

    private void handleAutoScroll(Intent intent) {
        String scrollToId = intent.getStringExtra("SCROLL_TO_REF_ID");
        String scrollToType = intent.getStringExtra("SCROLL_TO_REF_TYPE");

        if (scrollToId != null && scrollToType != null) {
            if (scrollToType.equals("COMPLAINT")) {
                binding.nestedScrollView.postDelayed(() -> {
                    binding.nestedScrollView.smoothScrollTo(0, binding.rvComplaintTimeline.getTop());
                }, 500);
                Toast.makeText(this, "Menampilkan referensi komplain...", Toast.LENGTH_SHORT).show();
            }
            else if (scrollToType.equals("MAINTENANCE")) {
                binding.nestedScrollView.postDelayed(() -> {
                    binding.nestedScrollView.smoothScrollTo(0, binding.rvMaintenanceTimeline.getTop());
                }, 500);
                Toast.makeText(this, "Menampilkan referensi perawatan...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==========================================
    // LOGIKA PEMBELI (BUYER)
    // ==========================================
    private void setupBuyerLogic() {
        buyerViewModel = new ViewModelProvider(this).get(BuyerMaintenanceViewModel.class);
        RentalDetailViewModel rentalViewModel = new ViewModelProvider(this).get(RentalDetailViewModel.class);

        rentalViewModel.getActiveRental().observe(this, rental -> {
            if (rental != null) {
                storeName = rental.getSellerName();
                receiverName = rental.getBuyerName();

                buyerId = rental.getBuyerId();
                sellerId = rental.getSellerId();

                timelineAdapter.setStoreName(storeName);

                complaintAdapter.setStoreName(storeName);
                complaintAdapter.setBuyerName(receiverName);
                complaintAdapter.setSellerMode(false);

                if (rental.getPlantImageUrl() != null && !rental.getPlantImageUrl().isEmpty()) {
                    Glide.with(this).load(rental.getPlantImageUrl()).into(binding.imgOriginalPlant);
                }
            }
        });
        rentalViewModel.fetchRentalAndTimeline(rentalId);

        binding.btnSubmitComplaint.setOnClickListener(v -> showComplaintDialog());
        if (binding.btnAcceptResolution != null) {
            binding.btnAcceptResolution.setOnClickListener(v -> {
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    Toast.makeText(this, "Koneksi terputus!", Toast.LENGTH_LONG).show();
                    return;
                }
                if (activeComplaintId != null) {
                    buyerComplaintViewModel.acceptResolution(rentalId, activeComplaintId);

                    binding.btnAcceptResolution.setText("Memproses...");
                    binding.btnAcceptResolution.setEnabled(false);
                    if (binding.btnRejectResolution != null) binding.btnRejectResolution.setEnabled(false);
                }
            });
        }

        if (binding.btnRejectResolution != null) {
            binding.btnRejectResolution.setOnClickListener(v -> {

                new AlertDialog.Builder(this)
                        .setTitle("Konfirmasi Penolakan")
                        .setMessage("Yakin ingin menolak perbaikan ini? Komplain akan dikembalikan ke penjual untuk diperbaiki ulang.")
                        .setPositiveButton("Ya, Tolak", (dialog, which) -> {

                            if (!NetworkUtils.isNetworkAvailable(this)) {
                                Toast.makeText(this, "Koneksi terputus!", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (activeComplaintId != null) {
                                buyerComplaintViewModel.rejectResolution(rentalId, activeComplaintId);

                                binding.btnRejectResolution.setText("Memproses...");
                                binding.btnRejectResolution.setEnabled(false);
                                binding.btnAcceptResolution.setEnabled(false);
                            }
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }

        buyerViewModel.getMaintenanceLogs().observe(this, logs -> {
            binding.progressBar.setVisibility(View.GONE);
            if (logs != null && !logs.isEmpty()) timelineAdapter.setLogs(logs);
        });

        buyerComplaintViewModel.getComplaintList().observe(this, complaints -> {
            if (complaints != null && !complaints.isEmpty()) {
                complaintAdapter.setComplaints(complaints);
                Complaint latest = complaints.get(complaints.size() - 1);
                String status = latest.getStatus();
                activeComplaintId = latest.getComplaintId();

                if ("Pending".equalsIgnoreCase(status) || "Responded".equalsIgnoreCase(status) || "Menunggu Konfirmasi".equalsIgnoreCase(status)) {

                    binding.layoutSubmitComplaint.setVisibility(View.GONE);

                    if ("Responded".equalsIgnoreCase(status) || "Menunggu Konfirmasi".equalsIgnoreCase(status)) {
                        binding.layoutConfirmResolution.setVisibility(View.VISIBLE);
                    } else {
                        binding.layoutConfirmResolution.setVisibility(View.GONE);
                    }
                } else {
                    binding.layoutSubmitComplaint.setVisibility(View.VISIBLE);
                    binding.layoutConfirmResolution.setVisibility(View.GONE);
                }
            } else {
                binding.layoutSubmitComplaint.setVisibility(View.VISIBLE);
                binding.layoutConfirmResolution.setVisibility(View.GONE);
            }
        });

        buyerComplaintViewModel.getIsSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                if(complaintDialog != null && complaintDialog.isShowing()) complaintDialog.dismiss();
                Toast.makeText(this, "Tindakan berhasil!", Toast.LENGTH_SHORT).show();

                if (binding.btnAcceptResolution != null) {
                    binding.btnAcceptResolution.setEnabled(true);
                }
            }
        });

        buyerComplaintViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility((isLoading != null && isLoading) ? View.VISIBLE : View.GONE);

            if (complaintBinding != null) {
                if (isLoading != null && isLoading) {
                    complaintBinding.btnSubmitComplaint.setText("Mengirim...");
                    complaintBinding.btnSubmitComplaint.setEnabled(false);
                } else {
                    complaintBinding.btnSubmitComplaint.setText("Kirim Komplain");
                    checkComplaintValidation();
                }
            }
        });

        buyerComplaintViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "GAGAL: " + error, Toast.LENGTH_LONG).show();

                if (binding.btnAcceptResolution != null) {
                    binding.btnAcceptResolution.setText("Terima Resolusi & Selesaikan");
                    binding.btnAcceptResolution.setEnabled(true);
                }
            }
        });
        buyerComplaintViewModel.fetchComplaints(rentalId);
        buyerComplaintViewModel.listenToDiscussion(rentalId);
        buyerViewModel.startListening(rentalId);
    }

    // ==========================================
    // LOGIKA PENJUAL (SELLER)
    // ==========================================
    private void setupSellerLogic() {
        sellerViewModel = new ViewModelProvider(this).get(RentalDetailViewModel.class);

        sellerViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility((isLoading != null && isLoading) ? View.VISIBLE : View.GONE);
        });

        sellerViewModel.getActiveRental().observe(this, rental -> {
            if (rental != null) {
                storeName = rental.getSellerName();
                receiverName = rental.getBuyerName();

                buyerId = rental.getBuyerId();
                sellerId = rental.getSellerId();

                timelineAdapter.setStoreName(storeName);

                complaintAdapter.setStoreName(storeName);
                complaintAdapter.setBuyerName(receiverName);
                complaintAdapter.setSellerMode(true);

                if (rental.getPlantImageUrl() != null && !rental.getPlantImageUrl().isEmpty()) {
                    Glide.with(this).load(rental.getPlantImageUrl()).into(binding.imgOriginalPlant);
                }
            }
        });

        sellerViewModel.getMaintenanceLogs().observe(this, logs -> {
            if (logs != null) timelineAdapter.setLogs(logs);
        });

        sellerViewModel.getComplaintList().observe(this, complaints -> {
            if (complaints != null && !complaints.isEmpty()) {
                complaintAdapter.setComplaints(complaints);
                Complaint latest = complaints.get(complaints.size() - 1);
                String status = latest.getStatus();

                if ("Pending".equalsIgnoreCase(status) || "Komplain".equalsIgnoreCase(status)) {
                    activeComplaintId = latest.getComplaintId();

                    // ---> TOMBOL JADWALKAN PERBAIKAN <---
                    if (binding.layoutReply != null) {
                        binding.layoutReply.setVisibility(View.VISIBLE);

                        // Sembunyikan kolom input ketik, karena kita hanya butuh klik tombolnya
                        if (binding.etSellerResponse != null) binding.etSellerResponse.setVisibility(View.GONE);

                        binding.btnSubmitResolution.setText("Jadwalkan Perbaikan Lapangan");
                        binding.btnSubmitResolution.setBackgroundColor(getResources().getColor(R.color.red_300));

                        binding.btnSubmitResolution.setOnClickListener(v -> {
                            // Ubah status ke PROSES PERBAIKAN (Otomatis masuk ke To-Do List Tukang Kebun)
                            sellerViewModel.updateComplaintStatus(rentalId, activeComplaintId, "PROSES PERBAIKAN");
                            Toast.makeText(this, "Jadwal diteruskan ke Tukang Kebun!", Toast.LENGTH_LONG).show();
                        });
                    }
                    // ------------------------------------

                } else if ("PROSES PERBAIKAN".equalsIgnoreCase(status)) {
                    activeComplaintId = latest.getComplaintId();
//                    sellerViewModel.listenToDiscussion(rentalId);

                    if (binding.layoutReply != null) binding.layoutReply.setVisibility(View.GONE);
                    Toast.makeText(this, "Tukang kebun sedang dijadwalkan ke lokasi.", Toast.LENGTH_SHORT).show();

                } else {
                    activeComplaintId = null;
//                    if (binding.layoutReply != null) binding.layoutReply.setVisibility(View.GONE);
                }
            }
        });

        sellerViewModel.getChatMessages().observe(this, messages -> {
        });

        sellerViewModel.listenToDiscussion(rentalId);
        sellerViewModel.fetchComplaintDetail(rentalId);
        sellerViewModel.fetchRentalAndTimeline(rentalId);
    }

    // ==========================================
    // FUNGSI PENDUKUNG DIALOG KOMPLAIN
    // ==========================================
    private void showComplaintDialog() {
        complaintDialog = new BottomSheetDialog(this);
        complaintBinding = DialogBuyerComplaintBinding.inflate(getLayoutInflater());
        complaintDialog.setContentView(complaintBinding.getRoot());
        complaintImageUri = null;

        String[] reasons = {"Tanaman Layu/Mati", "Pot Pecah/Rusak", "Perawatan Tidak Sesuai", "Lainnya"};
        complaintBinding.spinnerReason.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reasons));

        complaintBinding.imgComplaintPreview.setOnClickListener(v -> {
            String[] options = {"Ambil dari Kamera", "Pilih dari Galeri"};
            new AlertDialog.Builder(this).setTitle("Sumber Foto Bukti").setItems(options, (dialog, which) -> {
                if (which == 0) {
                    cameraUri = createImageUri();
                    takePicture.launch(cameraUri);
                } else {
                    pickComplaintMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                }
            }).show();
        });

        complaintBinding.etComplaintDescription.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkComplaintValidation(); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        complaintBinding.btnSubmitComplaint.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Tidak ada koneksi internet!", Toast.LENGTH_LONG).show();
                return;
            }

            complaintBinding.btnSubmitComplaint.setEnabled(false);
            complaintBinding.btnSubmitComplaint.setText("Memproses...");

            String reason = complaintBinding.spinnerReason.getSelectedItem().toString();
            String desc = complaintBinding.etComplaintDescription.getText().toString().trim();
            File safeImageFile = getFileFromUri(complaintImageUri);

            if (safeImageFile != null) {
                buyerComplaintViewModel.submitComplaint(rentalId, reason, desc, Uri.fromFile(safeImageFile));
            } else {
                complaintBinding.btnSubmitComplaint.setEnabled(true);
                complaintBinding.btnSubmitComplaint.setText("Kirim Komplain");
                Toast.makeText(this, "Gagal memproses gambar.", Toast.LENGTH_SHORT).show();
            }
        });
        complaintDialog.show();
    }

    private void checkComplaintValidation() {
        if (complaintBinding != null) {
            String text = complaintBinding.etComplaintDescription.getText().toString().trim();
            complaintBinding.btnSubmitComplaint.setEnabled((complaintImageUri != null) && (!text.isEmpty()));
        }
    }

    private Uri createImageUri() {
        File imageFile = new File(getCacheDir(), "komplain_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            File tempFile = new File(getCacheDir(), "safe_upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream os = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024]; int length;
            while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
            os.close(); is.close();
            return tempFile;
        } catch (Exception e) { return null; }
    }

    private void openChatRoomWithQuote(String title, String desc, String imageUrl, String refId, String refType) {
        if (buyerId == null || sellerId == null) {
            Toast.makeText(this, "Data pengguna belum dimuat, tunggu sebentar...", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RentalDetailActivity.this, ChatRoomActivity.class);

        String targetId = role.equals("SELLER") ? buyerId : sellerId;
        String targetName = role.equals("SELLER") ? receiverName : storeName;

        intent.putExtra("EXTRA_TARGET_ID", targetId);
        intent.putExtra("EXTRA_TARGET_NAME", targetName);

        String quoteText = "🌿 *" + title + "*\n\"" + desc + "\"\n";
        intent.putExtra("EXTRA_DRAFT_MESSAGE", quoteText);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            intent.putExtra("EXTRA_DRAFT_IMAGE", imageUrl);
        }

        intent.putExtra("EXTRA_DRAFT_REF_ID", refId);
        intent.putExtra("EXTRA_DRAFT_REF_TYPE", refType);
        intent.putExtra("EXTRA_DRAFT_RENTAL_ID", rentalId);

        Toast.makeText(this, "Membuka obrolan...", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    private void showZoomableImageDialog(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_zoom_layout);

        com.github.chrisbanes.photoview.PhotoView photoView = dialog.findViewById(R.id.photoView);
        android.widget.ImageButton btnClose = dialog.findViewById(R.id.btnCloseZoom);

        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .into(photoView);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}