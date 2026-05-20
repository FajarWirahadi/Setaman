package com.example.florist.views.seller;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.adapter.MaintenanceScheduleAdapter;
import com.example.florist.databinding.ActivityMaintenanceScheduleBinding;
import com.example.florist.databinding.DialogAddMaintenanceLogBinding;
import com.example.florist.model.Order;
import com.example.florist.viewmodels.MaintenanceViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.checkerframework.common.subtyping.qual.Bottom;

import java.util.List;

public class MaintenanceScheduleActivity extends AppCompatActivity {

    private ActivityMaintenanceScheduleBinding binding;
    private MaintenanceViewModel viewModel;
    private MaintenanceScheduleAdapter adapter;
    private ProgressDialog progressDialog;

    private Uri selectedImageUri = null;
    private BottomSheetDialog addLogDialog;
    private DialogAddMaintenanceLogBinding dialogBinding;

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




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMaintenanceScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MaintenanceViewModel.class);

        setupUI();
        setupObservers();

        viewModel.fetchOrdersInMaintenance();
    }


    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mengunggah laporan...");
        progressDialog.setCancelable(false);

        adapter = new MaintenanceScheduleAdapter(this, this::showAddDialog);
        binding.rvMaintenanceSchedule.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMaintenanceSchedule.setAdapter(adapter);

    }

    private void showAddDialog(Order order) {
        addLogDialog = new BottomSheetDialog(this);

        dialogBinding = DialogAddMaintenanceLogBinding.inflate(getLayoutInflater());
        addLogDialog.setContentView(dialogBinding.getRoot());

        selectedImageUri = null;
        dialogBinding.imgPreview.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        dialogBinding.etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkValidation();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        dialogBinding.btnSubmitLog.setOnClickListener(v -> {
            String description = dialogBinding.etDescription.getText().toString().trim();
            viewModel.AddMaintenance(order, selectedImageUri, description);
        });
        addLogDialog.show();
    }

    private void setupObservers() {
        viewModel.getOrdersInMaintenance().observe(this, orders -> {
            if (orders != null) {
                adapter.updateList(orders);
                updateSummary(orders);
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
                Toast.makeText(this, "message", Toast.LENGTH_SHORT).show();
                if (addLogDialog != null && !addLogDialog.isShowing()) {
                    addLogDialog.dismiss();
                }
                selectedImageUri = null;
            }
        });
    }

    private void updateSummary(List<Order> orders) {
        binding.tvTotalAll.setText(String.valueOf(orders.size()));
        binding.tvTotalFinished.setText("0");
        binding.tvTotalOngoing.setText(String.valueOf(orders.size()));
    }

    private void checkValidation() {
        if (dialogBinding != null) {
            String text = dialogBinding.etDescription.getText().toString().trim();
            boolean isValid = (selectedImageUri != null) && (!text.isEmpty());
            dialogBinding.btnSubmitLog.setEnabled(isValid);
        }
    }
}