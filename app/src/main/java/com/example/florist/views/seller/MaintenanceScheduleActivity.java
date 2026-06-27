package com.example.florist.views.seller;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.florist.adapter.CalendarAdapter;
import com.example.florist.adapter.MaintenanceScheduleAdapter;
import com.example.florist.databinding.ActivityMaintenanceScheduleBinding;
import com.example.florist.databinding.DialogAddMaintenanceLogBinding;
import com.example.florist.model.MaintenanceTaskUIModel;
import com.example.florist.model.Rental;
import com.example.florist.viewmodels.MaintenanceViewModel;
import com.example.florist.views.chat.ChatRoomActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaintenanceScheduleActivity extends AppCompatActivity {

    private ActivityMaintenanceScheduleBinding binding;
    private MaintenanceViewModel viewModel;
    private MaintenanceScheduleAdapter adapter;
    private CalendarAdapter calendarAdapter;
    private ProgressDialog progressDialog;

    private Uri selectedImageUri = null;
    private Uri cameraUri = null;
    private BottomSheetDialog addLogDialog;
    private DialogAddMaintenanceLogBinding dialogBinding;
    private List<Date> dateList = new ArrayList<>();

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (dialogBinding != null) {
                        Glide.with(this).load(uri).into(dialogBinding.imgPreview);
                    }
                    checkValidation();
                }
            });

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), isSuccess -> {
                if (isSuccess && cameraUri != null) {
                    selectedImageUri = cameraUri;
                    if (dialogBinding != null) {
                        Glide.with(this).load(selectedImageUri).into(dialogBinding.imgPreview);
                    }
                    checkValidation();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMaintenanceScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MaintenanceViewModel.class);

        setupUI();
        setupObservers();

        viewModel.fetchSellerRentals("SEWA AKTIF");
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mengunggah laporan...");
        progressDialog.setCancelable(false);

        adapter = new MaintenanceScheduleAdapter(this, new MaintenanceScheduleAdapter.OnMaintenanceListener() {
            @Override
            public void onAddLogClicked(MaintenanceTaskUIModel task) {
                if ("Tawarkan via Chat".equals(task.buttonText)) {
                    Intent chatIntent = new Intent(MaintenanceScheduleActivity.this, ChatRoomActivity.class);
                    chatIntent.putExtra("EXTRA_TARGET_ID", task.rental.getBuyerId());
                    chatIntent.putExtra("EXTRA_TARGET_NAME", task.rental.getBuyerName());
                    chatIntent.putExtra("EXTRA_TARGET_IMAGE", task.rental.getPlantImageUrl());
                    String quoteText = "🌿 *Pemberitahuan Kontrak*\n\"Halo kak, masa sewa " + task.rental.getPlantName() + " tinggal sebentar lagi lho! Apakah ingin diperpanjang?\"\n";
                    chatIntent.putExtra("EXTRA_DRAFT_MESSAGE", quoteText);
                    chatIntent.putExtra("EXTRA_DRAFT_REF_ID", task.rental.getRentalId());
                    chatIntent.putExtra("EXTRA_DRAFT_REF_TYPE", "RENTAL");
                    chatIntent.putExtra("EXTRA_DRAFT_RENTAL_ID", task.rental.getRentalId());
                    startActivity(chatIntent);
                } else {
                    showAddDialog(task); // Munculkan dialog unggah bukti
                }
            }

            @Override
            public void onCardClicked(Rental rental) {
                Intent intent = new Intent(MaintenanceScheduleActivity.this, RentalDetailActivity.class);
                intent.putExtra("RENTAL_ID", rental.getRentalId());
                intent.putExtra("ORDER_ID", rental.getOrderId());
                intent.putExtra("STORE_NAME", rental.getSellerName());
                intent.putExtra("ROLE", "SELLER");
                startActivity(intent);
            }

            @Override
            public void onChatClicked(Rental rental) {
                Intent chatIntent = new Intent(MaintenanceScheduleActivity.this, ChatRoomActivity.class);
                chatIntent.putExtra("EXTRA_TARGET_ID", rental.getBuyerId());
                chatIntent.putExtra("EXTRA_TARGET_NAME", rental.getBuyerName());
                chatIntent.putExtra("EXTRA_TARGET_IMAGE", rental.getPlantImageUrl());
                String quoteText = "🌿 *Jadwal Perawatan*\n\"Sewa ID: " + rental.getOrderId() + " - " + rental.getPlantName() + "\"\n";
                chatIntent.putExtra("EXTRA_DRAFT_MESSAGE", quoteText);
                chatIntent.putExtra("EXTRA_DRAFT_IMAGE", rental.getPlantImageUrl());
                chatIntent.putExtra("EXTRA_DRAFT_REF_ID", rental.getRentalId());
                chatIntent.putExtra("EXTRA_DRAFT_REF_TYPE", "MAINTENANCE");
                chatIntent.putExtra("EXTRA_DRAFT_RENTAL_ID", rental.getRentalId());
                startActivity(chatIntent);
            }
        });

        binding.rvMaintenanceSchedule.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMaintenanceSchedule.setAdapter(adapter);

        setupCalendar();
        // binding.btnTabOngoing.setOnClickListener(v -> viewModel.fetchSellerRentals("AKTIF"));
        // binding.btnTabHistory.setOnClickListener(v -> viewModel.fetchSellerRentals("SELESAI"));
    }

    private void showAddDialog(MaintenanceTaskUIModel task) {
        addLogDialog = new BottomSheetDialog(this);

        dialogBinding = DialogAddMaintenanceLogBinding.inflate(getLayoutInflater());
        addLogDialog.setContentView(dialogBinding.getRoot());

        selectedImageUri = null;
        dialogBinding.imgPreview.setOnClickListener(v -> {
            String[] options = {"Ambil dari Kamera", "Pilih dari Galeri"};
            AlertDialog alertDialog = new  AlertDialog.Builder(this)
                    .setTitle("Sumber Foto Bukti")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // Buka Kamera
                            cameraUri = createImageUri();
                            takePicture.launch(cameraUri);
                        } else {
                            pickMedia.launch(new PickVisualMediaRequest.Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                    .build());
                        }
                    }).create();

                    if (alertDialog.getWindow() != null) {
                        alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
                    }
                    alertDialog.show();
        });
        dialogBinding.etDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { checkValidation(); }
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable editable) {}
        });

        dialogBinding.btnSubmitLog.setOnClickListener(v -> {
            String description = dialogBinding.etDescription.getText().toString().trim();
            viewModel.AddMaintenance(task.rental, selectedImageUri, description, task.activeComplaintId);
        });
        addLogDialog.show();
    }

    private Uri createImageUri() {
        File imageFile = new File(getCacheDir(), "perawatan_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
    }

    private void setupObservers() {
        viewModel.getFilteredTasks().observe(this, tasks -> {
            if (tasks != null) {
                adapter.updateList(tasks);
            }
        });

        viewModel.getCalendarIndicators().observe(this, indicators -> {
            if (indicators != null && calendarAdapter != null) {
                calendarAdapter.setTaskIndicators(indicators.routineDates, indicators.complaintDates);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                progressDialog.show();
            } else {
                progressDialog.dismiss();
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        viewModel.getActionSuccessMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                if (addLogDialog != null && addLogDialog.isShowing()) {
                    addLogDialog.dismiss();
                }
                selectedImageUri = null;
            }
        });
    }

//    private void updateSummary(List<Rental> rentals) {
//        binding.tvTotalAll.setText(String.valueOf(rentals.size()));
//        binding.tvTotalFinished.setText("0"); // Anda bisa menghitung yang statusnya 'SELESAI' nanti
//        binding.tvTotalOngoing.setText(String.valueOf(rentals.size()));
//    }

    private void checkValidation() {
        if (dialogBinding != null) {
            String text = dialogBinding.etDescription.getText().toString().trim();
            boolean isValid = (selectedImageUri != null) && (!text.isEmpty());
            dialogBinding.btnSubmitLog.setEnabled(isValid);
        }
    }

    private void setupCalendar() {
        dateList.clear();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 14; i++) {
            dateList.add(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        viewModel.setSelectedDate(dateList.get(0));

        calendarAdapter = new CalendarAdapter(dateList, 0, new CalendarAdapter.OnDateClickListener() {
            @Override
            public void onDateClick(Date date) {
                viewModel.setSelectedDate(date);
            }
        });

        binding.rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvCalendar.setAdapter(calendarAdapter);

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));
        binding.tvCurrentMonth.setText(monthFormat.format(dateList.get(0)));
    }
}