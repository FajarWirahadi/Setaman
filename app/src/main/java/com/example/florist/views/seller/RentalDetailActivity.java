package com.example.florist.views.seller;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.adapter.UnifiedTimelineAdapter;
import com.example.florist.databinding.ActivityRentalDetailBinding;
import com.example.florist.databinding.DialogBuyerComplaintBinding;
import com.example.florist.model.Complaint;
import com.example.florist.model.TimelineEvent;
import com.example.florist.utils.Constants;
import com.example.florist.utils.NetworkUtils;
import com.example.florist.viewmodels.ComplaintViewModel;
import com.example.florist.viewmodels.RentalDetailViewModel;
import com.example.florist.views.chat.ChatRoomActivity;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RentalDetailActivity extends AppCompatActivity {

    private ActivityRentalDetailBinding binding;

    private String rentalId, orderId, storeName, receiverName;
    private String activeRentalDuration = "-";
    private String activeBuyerImageUrl = "";
    private String activePlantImageUrl = "";
    private String activePlantName = "Tanaman";
    private String pendingScrollId = null;
    private String activeComplaintImageUrl = null;
    private String buyerId, sellerId;
    private String currentUid;

    private ComplaintViewModel complaintViewModel;
    private RentalDetailViewModel rentalViewModel;

    private UnifiedTimelineAdapter unifiedTimelineAdapter;

    private String activeComplaintId = null;
    private BottomSheetDialog complaintDialog;
    private DialogBuyerComplaintBinding complaintBinding;
    private Uri complaintImageUri = null;
    private Uri cameraUri;
    private boolean isBuyerSpecificsSetup = false;
    private boolean isSellerSpecificsSetup = false;

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

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        complaintViewModel = new ViewModelProvider(this).get(ComplaintViewModel.class);
        rentalViewModel = new ViewModelProvider(this).get(RentalDetailViewModel.class);

        rentalId = getIntent().getStringExtra("RENTAL_ID");
        orderId = getIntent().getStringExtra("ORDER_ID");
        pendingScrollId = getIntent().getStringExtra("SCROLL_TO_REF_ID");

        if (rentalId == null) {
            Toast.makeText(this, "Data pesanan tidak lengkap.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupCommonUI();
        setupChatButton();
        setupMainObservers();

        rentalViewModel.fetchRentalAndTimeline(rentalId);
        rentalViewModel.fetchUnifiedTimeline(rentalId);
    }

    private void setupCommonUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

//        getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
//                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//        );
//        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        binding.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (Math.abs(verticalOffset) - appBarLayout.getTotalScrollRange() == 0) {
                binding.collapsingToolbar.setTitle("Riwayat Perawatan");
                if (binding.toolbar.getNavigationIcon() != null) binding.toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
            } else {
                binding.collapsingToolbar.setTitle("");
                if (binding.toolbar.getNavigationIcon() != null) binding.toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.main_color));
            }
        });

        unifiedTimelineAdapter = new UnifiedTimelineAdapter(new UnifiedTimelineAdapter.OnTimelineActionListener() {
            @Override
            public void onQuoteClicked(TimelineEvent event) {
                String dateStr = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(event.getTimestamp().toDate());
                String title = (event.getEventType() == TimelineEvent.TYPE_COMPLAINT) ? "Komplain (" + dateStr + ")" : "Laporan Perawatan (" + dateStr + ")";
                String refType = (event.getEventType() == TimelineEvent.TYPE_COMPLAINT) ? "COMPLAINT" : "MAINTENANCE";
                openChatRoomWithQuote(title, event.getDescription(), event.getImageUrl(), event.getEventId(), refType);
            }
            @Override
            public void onImageZoomClicked(String imageUrl) {
                showZoomableImageDialog(imageUrl);
            }
        });

        binding.rvUnifiedTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUnifiedTimeline.setAdapter(unifiedTimelineAdapter);
        binding.rvUnifiedTimeline.setItemAnimator(null);
    }

    private void setupChatButton() {
        binding.btnSendChat.setOnClickListener(v -> {
            String text = binding.etChatMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            boolean isSeller = currentUid.equals(sellerId);
            complaintViewModel.sendChatMessage(rentalId, isSeller ? "Penjual" : "Pembeli", isSeller ? storeName : receiverName, "", text);
            binding.etChatMessage.setText("");
        });
    }

    private void setupMainObservers() {
        binding.progressBar.setVisibility(View.VISIBLE);

        rentalViewModel.getActiveRental().observe(this, rental -> {
            if (rental != null) {
                binding.progressBar.setVisibility(View.GONE);

                storeName = rental.getSellerName();
                receiverName = rental.getBuyerName();
                buyerId = rental.getBuyerId();
                sellerId = rental.getSellerId();
                activePlantName = rental.getPlantName();

                if (rental.getStartDate() != null && rental.getEndDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
                    activeRentalDuration = sdf.format(rental.getStartDate().toDate()) + " - " + sdf.format(rental.getEndDate().toDate());
                }

                if (rental.getPlantImageUrl() != null && !rental.getPlantImageUrl().isEmpty()) {
                    activePlantImageUrl = rental.getPlantImageUrl();
                    Glide.with(this).load(rental.getPlantImageUrl()).into(binding.imgOriginalPlant);
                }

                boolean isSeller = currentUid.equals(sellerId);
                unifiedTimelineAdapter.setStoreName(storeName);
                unifiedTimelineAdapter.setBuyerName(receiverName);
                unifiedTimelineAdapter.setSellerMode(isSeller);

                if (isSeller) {
                    if (!isSellerSpecificsSetup) {
                        setupSellerSpecifics();
                        isSellerSpecificsSetup = true;
                    }
                } else {
                    if (!isBuyerSpecificsSetup) {
                        setupBuyerSpecifics();
                        isBuyerSpecificsSetup = true;
                    }
                }
            }
        });

        rentalViewModel.getUnifiedTimeline().observe(this, events -> {
            if (events != null) unifiedTimelineAdapter.setEvents(events);
            if (pendingScrollId != null) {
                scrollToTargetEvent(pendingScrollId);
                pendingScrollId = null;
            }
        });

        rentalViewModel.getShowExtensionBanner().observe(this, show -> {
            // Tampilkan UI Banner Oranye (Pastikan Anda sudah menempelkan kode XML nya dari respon sebelumnya)
            if (binding.layoutExtensionBanner != null) {
                binding.layoutExtensionBanner.setVisibility((show != null && show) ? View.VISIBLE : View.GONE);
            }
        });

        rentalViewModel.getExtensionDaysText().observe(this, text -> {
            if (text != null && binding.tvExtensionTitle != null) {
                binding.tvExtensionTitle.setText(text);
            }
        });

        rentalViewModel.getMidtransRedirectUrl().observe(this, url -> {
            if (url != null && !url.isEmpty()) {
                Toast.makeText(this, "Membuka halaman pembayaran...", Toast.LENGTH_SHORT).show();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        if (binding.btnExtendRental != null) {
            binding.btnExtendRental.setOnClickListener(v -> {
                showExtensionCheckoutDialog(); // Buka Kasir BottomSheet
            });
        }

        rentalViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility((isLoading != null && isLoading) ? View.VISIBLE : View.GONE);
        });
    }

    private void setupBuyerSpecifics() {
        binding.btnSubmitComplaint.setOnClickListener(v -> showComplaintDialog());

        if (binding.btnAcceptResolution != null) {
            binding.btnAcceptResolution.setOnClickListener(v -> {
                if (!NetworkUtils.isNetworkAvailable(this)) return;
                if (activeComplaintId != null) {
                    complaintViewModel.acceptResolution(activeComplaintId);
                    binding.btnAcceptResolution.setText("Memproses...");
                    binding.btnAcceptResolution.setEnabled(false);
                    if (binding.btnRejectResolution != null) binding.btnRejectResolution.setEnabled(false);
                }
            });
        }

        if (binding.btnRejectResolution != null) {
            binding.btnRejectResolution.setOnClickListener(v -> {
                android.widget.FrameLayout container = new android.widget.FrameLayout(this);
                android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = 50; params.rightMargin = 50;
                android.widget.EditText inputReason = new android.widget.EditText(this);
                inputReason.setHint("Ketik alasan penolakan di sini...");
                inputReason.setLayoutParams(params);
                container.addView(inputReason);

                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setTitle("Tolak Resolusi")
                        .setMessage("Yakin ingin menolak resolusi ini?")
                        .setView(container)
                        .setPositiveButton("Ya, Tolak", (dialog, which) -> {
                            if (!NetworkUtils.isNetworkAvailable(this)) return;
                            if (activeComplaintId != null) {
                                String typedReason = inputReason.getText().toString().trim();
                                if (typedReason.isEmpty()) typedReason = "Pembeli menolak tanpa memberikan alasan spesifik.";
                                complaintViewModel.rejectResolution(activeComplaintId, typedReason);
                                binding.btnRejectResolution.setText("Memproses...");
                                binding.btnRejectResolution.setEnabled(false);
                                binding.btnAcceptResolution.setEnabled(false);
                            }
                        })
                        .setNegativeButton("Batal", null)
                        .create();

                        if (alertDialog.getWindow() != null) {
                            alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
                            }
                        alertDialog.show();
            });
        }

        complaintViewModel.getComplaintList().observe(this, complaints -> {
            if (complaints != null && !complaints.isEmpty()) {
                Complaint latest = complaints.get(complaints.size() - 1);
                String rawStatus = latest.getStatus();
                if (rawStatus == null) return;

                String status = rawStatus.toUpperCase();
                activeComplaintId = latest.getComplaintId();

                if (status.equals(Constants.ORDER_PENDING) ||
                        status.equals(Constants.COMPLAINT_WAITING_RESPONSE) ||
                        status.equals(Constants.COMPLAINT_WAITING_CONFIRM) ||
                        status.equals(Constants.COMPLAINT_PROCESSING) ||
                        status.equals(Constants.COMPLAINT_MANDATORY_VISIT)) {

                    binding.layoutSubmitComplaint.setVisibility(View.GONE);
                    binding.layoutConfirmResolution.setVisibility(status.equals(Constants.COMPLAINT_WAITING_CONFIRM) ? View.VISIBLE : View.GONE);
                } else {
                    binding.layoutSubmitComplaint.setVisibility(View.VISIBLE);
                    binding.layoutConfirmResolution.setVisibility(View.GONE);
                }
            } else {
                binding.layoutSubmitComplaint.setVisibility(View.VISIBLE);
                binding.layoutConfirmResolution.setVisibility(View.GONE);
            }
        });

        complaintViewModel.getIsSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                ((MutableLiveData<Boolean>) complaintViewModel.getIsSuccess()).setValue(false);

                if (complaintDialog != null && complaintDialog.isShowing()) {
                    complaintDialog.dismiss();
                }

                Toast.makeText(this, "Aksi berhasil diproses!", Toast.LENGTH_SHORT).show();

                if (complaintBinding != null) {
                    complaintBinding.btnSubmitComplaint.setEnabled(true);
                    complaintBinding.btnSubmitComplaint.setText("Kirim Komplain");
                }
                if (binding.btnAcceptResolution != null) {
                    binding.btnAcceptResolution.setEnabled(true);
                    binding.btnAcceptResolution.setText("Terima Resolusi");
                }
                if (binding.btnRejectResolution != null) {
                    binding.btnRejectResolution.setEnabled(true);
                    binding.btnRejectResolution.setText("Tolak");
                }

                if (rentalViewModel != null && rentalId != null) {
                    rentalViewModel.fetchRentalAndTimeline(rentalId);
                    rentalViewModel.fetchUnifiedTimeline(rentalId);
                }
            }
        });
        complaintViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();

                if (complaintBinding != null) {
                    complaintBinding.btnSubmitComplaint.setEnabled(true);
                    complaintBinding.btnSubmitComplaint.setText("Kirim Komplain");
                }

                if (binding.btnAcceptResolution != null) {
                    binding.btnAcceptResolution.setEnabled(true);
                    binding.btnAcceptResolution.setText("Terima Resolusi");
                }
                if (binding.btnRejectResolution != null) {
                    binding.btnRejectResolution.setEnabled(true);
                    binding.btnRejectResolution.setText("Tolak");
                }
            }
        });

        complaintViewModel.fetchComplaints(rentalId);
    }
    private void setupSellerSpecifics() {
        rentalViewModel.getComplaintList().observe(this, complaints -> {
            if (complaints != null && !complaints.isEmpty()) {
                Complaint latest = complaints.get(complaints.size() - 1);
                String rawStatus = latest.getStatus();
                if (rawStatus == null) return;

                String status = rawStatus.toUpperCase();

                activeComplaintId = latest.getComplaintId();
                activeComplaintImageUrl = latest.getEvidenceImageUrl();

                if (status.equals(com.example.florist.utils.Constants.COMPLAINT_WAITING_RESPONSE) ||
                        status.equals("KOMPLAIN") || // Fallback jika ada data lama di Firebase
                        status.equals(com.example.florist.utils.Constants.ORDER_PENDING) ||
                        status.equals(com.example.florist.utils.Constants.COMPLAINT_MANDATORY_VISIT)) {

                    if (binding.layoutReply != null) {
                        binding.layoutReply.setVisibility(View.VISIBLE);
                        if (status.equals(com.example.florist.utils.Constants.COMPLAINT_MANDATORY_VISIT)) {
                            binding.btnActionRespond.setText("Jadwalkan Kunjungan (Wajib)");
                            binding.btnActionRespond.setBackgroundColor(getResources().getColor(R.color.red_500));
                        } else {
                            binding.btnActionRespond.setText("Tanggapi Komplain");
                            binding.btnActionRespond.setBackgroundColor(getResources().getColor(R.color.main_color));
                        }
                        binding.btnActionRespond.setOnClickListener(v -> showSellerResolutionDialog(status));
                    }
                } else if (status.equals(com.example.florist.utils.Constants.COMPLAINT_PROCESSING)) {
                    activeComplaintId = latest.getComplaintId();
                    if (binding.layoutReply != null) binding.layoutReply.setVisibility(View.GONE);
                    Toast.makeText(this, "Florist sedang dijadwalkan ke lokasi.", Toast.LENGTH_SHORT).show();
                } else {
                    activeComplaintId = null;
                    if (binding.layoutReply != null) binding.layoutReply.setVisibility(View.GONE);
                }
            }
        });

        rentalViewModel.fetchComplaintDetail(rentalId);
    }
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
            if (!NetworkUtils.isNetworkAvailable(this)) return;
            complaintBinding.btnSubmitComplaint.setEnabled(false);
            complaintBinding.btnSubmitComplaint.setText("Memproses...");

            String reason = complaintBinding.spinnerReason.getSelectedItem().toString();
            String desc = complaintBinding.etComplaintDescription.getText().toString().trim();
            File safeImageFile = getFileFromUri(complaintImageUri);

            if (safeImageFile != null) {
                complaintViewModel.submitComplaint(rentalId, orderId, activeRentalDuration, buyerId, activeBuyerImageUrl, sellerId, activePlantName, receiverName, reason, desc, Uri.fromFile(safeImageFile));
            } else {
                complaintBinding.btnSubmitComplaint.setEnabled(true);
                complaintBinding.btnSubmitComplaint.setText("Kirim Komplain");
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
        if (buyerId == null || sellerId == null) return;
        Intent intent = new Intent(RentalDetailActivity.this, ChatRoomActivity.class);
        boolean isSeller = currentUid.equals(sellerId);
        intent.putExtra("EXTRA_TARGET_ID", isSeller ? buyerId : sellerId);
        intent.putExtra("EXTRA_TARGET_NAME", isSeller ? receiverName : storeName);
        intent.putExtra("EXTRA_DRAFT_MESSAGE", "🌿 *" + title + "*\n\"" + desc + "\"\n");
        if (imageUrl != null && !imageUrl.isEmpty()) intent.putExtra("EXTRA_DRAFT_IMAGE", imageUrl);
        intent.putExtra("EXTRA_DRAFT_REF_ID", refId);
        intent.putExtra("EXTRA_DRAFT_REF_TYPE", refType);
        intent.putExtra("EXTRA_DRAFT_RENTAL_ID", rentalId);
        startActivity(intent);
    }

    private void showSellerResolutionDialog(String currentStatus) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_seller_resolution, null);
        dialog.setContentView(view);
        View btnOptChat = view.findViewById(R.id.btnOptChat);
        View btnOptVisit = view.findViewById(R.id.btnOptVisit);

        if (Constants.COMPLAINT_MANDATORY_VISIT.equals(currentStatus)) {
            btnOptChat.setVisibility(View.GONE);
        }

        btnOptChat.setOnClickListener(v -> {
            dialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle("Beri Panduan via Chat")
                    .setMessage("Status Komplain akan diubah menjadi 'MENUNGGU KONFIRMASI'. Lanjutkan?")
                    .setPositiveButton("Ya", (d, which) -> {
                        rentalViewModel.updateComplaintStatus(rentalId, activeComplaintId, Constants.COMPLAINT_WAITING_CONFIRM, "CHAT_EDUCATION");
                        openChatRoomWithQuote("Panduan Penanganan Tanaman", "Halo kak, ikuti langkah berikut: ", activeComplaintImageUrl, activeComplaintId, "COMPLAINT");
                    }).setNegativeButton("Batal", null).show();
        });

        btnOptVisit.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, MaintenanceScheduleActivity.class);
            intent.putExtra("EXTRA_RENTAL_ID", rentalId);
            intent.putExtra("EXTRA_COMPLAINT_ID", activeComplaintId);
            intent.putExtra("EXTRA_IS_COMPLAINT_VISIT", true);
            startActivity(intent);
        });
        dialog.show();
    }

    private void showZoomableImageDialog(String imageUrl) {
        Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_zoom_layout);
        PhotoView photoView = dialog.findViewById(R.id.photoView);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseZoom);
        Glide.with(this).load(imageUrl).into(photoView);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void scrollToTargetEvent(String eventId) {
        int position = unifiedTimelineAdapter.getPositionByEventId(eventId);
        if (position == -1) return;
        if (binding.appBarLayout != null) binding.appBarLayout.setExpanded(false, true);

        binding.rvUnifiedTimeline.postDelayed(() -> {
            View targetView = binding.rvUnifiedTimeline.getLayoutManager().findViewByPosition(position);
            if (targetView != null) {
                int rvTop = binding.rvUnifiedTimeline.getTop();
                int viewY = (int) targetView.getY();
                int screenCenter = binding.nestedScrollView.getHeight() / 2;
                int viewCenter = targetView.getHeight() / 2;
                int scrollY = Math.max(0, rvTop + viewY - screenCenter + viewCenter);
                binding.nestedScrollView.smoothScrollTo(0, scrollY);
                targetView.setPressed(true);
                targetView.postDelayed(() -> targetView.setPressed(false), 1500);
            } else {
                ((LinearLayoutManager) binding.rvUnifiedTimeline.getLayoutManager()).scrollToPositionWithOffset(position, binding.rvUnifiedTimeline.getHeight() / 2);
            }
        }, 350);
    }
    private void showExtensionCheckoutDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_to_cart, null);
        dialog.setContentView(view);

        android.widget.ImageButton btnClose = view.findViewById(R.id.btnClose);
        android.widget.ImageView imgDialogProduct = view.findViewById(R.id.imgDialogProduct);
        android.widget.TextView tvDialogName = view.findViewById(R.id.tvDialogName);
        android.widget.TextView tvDialogPrice = view.findViewById(R.id.tvDialogPrice);
        android.widget.TextView tvDialogStock = view.findViewById(R.id.tvDialogStock);

        android.widget.Button btnTypeHarian = view.findViewById(R.id.btnTypeHarian);
        android.widget.Button btnTypeMingguan = view.findViewById(R.id.btnTypeMingguan);
        android.widget.Button btnTypeBulanan = view.findViewById(R.id.btnTypeBulanan);

        com.google.android.material.button.MaterialButton btnMinDuration = view.findViewById(R.id.btnMinDuration);
        com.google.android.material.button.MaterialButton btnAddDuration = view.findViewById(R.id.btnAddDuration);
        android.widget.TextView tvDurationValue = view.findViewById(R.id.tvDurationValue);
        android.widget.TextView tvDialogTotalPrice = view.findViewById(R.id.tvDialogTotalPrice);
        android.widget.Button btnSubmitCart = view.findViewById(R.id.btnSubmitCart);

        // 1. MANIPULASI UI (Sembunyikan elemen yang tidak perlu)
        tvDialogStock.setVisibility(View.GONE);
        btnSubmitCart.setText("Bayar Perpanjangan via Midtrans");

        view.findViewById(R.id.btnMinQty).setOnClickListener(v -> Toast.makeText(this, "Jumlah tanaman tetap 1 untuk perpanjangan.", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnAddQty).setOnClickListener(v -> Toast.makeText(this, "Jumlah tanaman tetap 1 untuk perpanjangan.", Toast.LENGTH_SHORT).show());

        tvDialogName.setText(activePlantName);
        if (!activePlantImageUrl.isEmpty()) {
            Glide.with(this).load(activePlantImageUrl).into(imgDialogProduct);
        }

        // 3. VARIABEL HARGA
        final int[] baseDays = {30};
        final double[] basePrice = {100000.0};
        final int[] durationMultiplier = {1};

        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        Runnable updateUI = () -> {
            tvDurationValue.setText(String.valueOf(durationMultiplier[0]));
            double total = basePrice[0] * durationMultiplier[0];
            tvDialogTotalPrice.setText(formatter.format(total));
            String labelDurasi = (baseDays[0] == 1) ? "/hari" : (baseDays[0] == 7) ? "/minggu" : "/bulan";
            tvDialogPrice.setText(formatter.format(basePrice[0]) + " " + labelDurasi);
        };

        // 4. LOGIKA TOMBOL PILIHAN PAKET
        View.OnClickListener typeListener = v -> {
            btnTypeHarian.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.white)));
            btnTypeHarian.setTextColor(getResources().getColor(R.color.gray_700));
            btnTypeMingguan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.white)));
            btnTypeMingguan.setTextColor(getResources().getColor(R.color.gray_700));
            btnTypeBulanan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.white)));
            btnTypeBulanan.setTextColor(getResources().getColor(R.color.gray_700));

            v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.olive_500)));
            ((android.widget.Button)v).setTextColor(getResources().getColor(R.color.white));

            if (v.getId() == R.id.btnTypeHarian) {
                baseDays[0] = 1; basePrice[0] = 5000.0;
            } else if (v.getId() == R.id.btnTypeMingguan) {
                baseDays[0] = 7; basePrice[0] = 30000.0;
            } else if (v.getId() == R.id.btnTypeBulanan) {
                baseDays[0] = 30; basePrice[0] = 100000.0;
            }

            durationMultiplier[0] = 1;
            updateUI.run();
        };

        btnTypeHarian.setOnClickListener(typeListener);
        btnTypeMingguan.setOnClickListener(typeListener);
        btnTypeBulanan.setOnClickListener(typeListener);

        btnAddDuration.setOnClickListener(v -> { durationMultiplier[0]++; updateUI.run(); });
        btnMinDuration.setOnClickListener(v -> {
            if (durationMultiplier[0] > 1) { durationMultiplier[0]--; updateUI.run(); }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 6. EKSEKUSI PEMBAYARAN
        btnSubmitCart.setOnClickListener(v -> {
            dialog.dismiss();
            com.example.florist.model.Rental currentRental = rentalViewModel.getActiveRental().getValue();
            if (currentRental != null) {
                int finalDays = baseDays[0] * durationMultiplier[0];
                double finalPrice = basePrice[0] * durationMultiplier[0];
                rentalViewModel.requestExtensionPayment(currentRental, finalPrice, finalDays);
            }
        });

        btnTypeBulanan.performClick();
        dialog.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingScrollId = intent.getStringExtra("SCROLL_TO_REF_ID");
    }
}