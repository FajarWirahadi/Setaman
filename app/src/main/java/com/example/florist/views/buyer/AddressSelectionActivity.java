package com.example.florist.views.buyer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.AddressAdapter;
import com.example.florist.databinding.ActivityAddressSelectionBinding;
import com.example.florist.databinding.DialogFormAddressBinding;
import com.example.florist.model.DeliveryAddress;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddressSelectionActivity extends AppCompatActivity implements AddressAdapter.AddressClickListener {

    private ActivityAddressSelectionBinding binding;
    private AddressAdapter adapter;
    private List<DeliveryAddress> addressList;
    private FirebaseFirestore db;
    private String currentUserId;
    private DeliveryAddress currentSelectedAddress = null;

    private DialogFormAddressBinding currentFormBinding;
    private double tempLat = 0.0;
    private double tempLng = 0.0;

    // PELUNCUR MAPS
    private final ActivityResultLauncher<Intent> mapsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    tempLat = result.getData().getDoubleExtra("LATITUDE", 0.0);
                    tempLng = result.getData().getDoubleExtra("LONGITUDE", 0.0);
                    String address = result.getData().getStringExtra("ADDRESS");

                    // Jika bottom sheet sedang terbuka, langsung isikan teks alamatnya!
                    if (currentFormBinding != null) {
                        currentFormBinding.etFullAddress.setText(address);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        setupUI();
        loadAddresses();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnAddAddress.setOnClickListener(v -> showAddressFormDialog(null));

        addressList = new ArrayList<>();
        adapter = new AddressAdapter(this, addressList, this);
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);

        binding.btnConfirmSelection.setEnabled(false);
        binding.btnConfirmSelection.setAlpha(0.5f);

        binding.btnConfirmSelection.setOnClickListener(v -> {
            if (currentSelectedAddress != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("SELECTED_ADDRESS", currentSelectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void loadAddresses() {
        db.collection("users").document(currentUserId).collection("addresses")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Gagal memuat alamat", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        addressList.clear();
                        int index = 0;
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            DeliveryAddress address = doc.toObject(DeliveryAddress.class);
                            if (address != null) {
                                addressList.add(address);
                                if (address.isMainAddress() && currentSelectedAddress == null) {
                                    currentSelectedAddress = address;
                                    adapter.setSelectedPosition(index);
                                    enableConfirmButton();
                                }
                                index++;
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void enableConfirmButton() {
        binding.btnConfirmSelection.setEnabled(true);
        binding.btnConfirmSelection.setAlpha(1.0f);
    }

    @Override
    public void onAddressClick(DeliveryAddress address, int position) {
        currentSelectedAddress = address;
        enableConfirmButton();
    }

    @Override
    public void onEditClick(DeliveryAddress address, int position) {
        showAddressFormDialog(address);
    }

    private void showAddressFormDialog(DeliveryAddress existingAddress) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        // Simpan referensi form ke variabel global agar bisa diakses oleh mapLauncher
        currentFormBinding = DialogFormAddressBinding.inflate(getLayoutInflater());
        dialog.setContentView(currentFormBinding.getRoot());

        boolean isEdit = (existingAddress != null);

        if (isEdit) {
            currentFormBinding.tvFormTitle.setText("Ubah Alamat");
            currentFormBinding.etLabel.setText(existingAddress.getLabel());
            currentFormBinding.etReceiverName.setText(existingAddress.getReceiverName());
            currentFormBinding.etPhone.setText(existingAddress.getPhoneNumber());
            currentFormBinding.etFullAddress.setText(existingAddress.getFullAddress());
            currentFormBinding.cbMainAddress.setChecked(existingAddress.isMainAddress());

            if (existingAddress.getNote() != null) {
                currentFormBinding.etNote.setText(existingAddress.getNote());
            }

            currentFormBinding.cbMainAddress.setChecked(existingAddress.isMainAddress());
            tempLat = existingAddress.getLatitude();
            tempLng = existingAddress.getLongitude();
        } else {
            // Reset koordinat untuk alamat baru
            tempLat = 0.0;
            tempLng = 0.0;
        }

        currentFormBinding.btnPickMap.setOnClickListener(v -> {
            Intent intent = new Intent(AddressSelectionActivity.this, com.example.florist.views.seller.createshop.MapsActivity.class);
            mapsLauncher.launch(intent);
        });

        currentFormBinding.btnSaveAddress.setOnClickListener(v -> {
            String label = currentFormBinding.etLabel.getText().toString().trim();
            String name = currentFormBinding.etReceiverName.getText().toString().trim();
            String phone = currentFormBinding.etPhone.getText().toString().trim();
            String fullAddress = currentFormBinding.etFullAddress.getText().toString().trim();
            String note = currentFormBinding.etNote.getText().toString().trim();
            boolean isMain = currentFormBinding.cbMainAddress.isChecked();

            if (label.isEmpty() || name.isEmpty() || phone.isEmpty() || fullAddress.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib (kecuali catatan) harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            String addressId = isEdit ? existingAddress.getAddressId() : UUID.randomUUID().toString();

            // SUNTIKAN KOORDINAT KE KOPER
            DeliveryAddress newAddress = new DeliveryAddress(addressId, label, name, phone, fullAddress, note, isMain, tempLat, tempLng);

            db.collection("users").document(currentUserId)
                    .collection("addresses").document(addressId)
                    .set(newAddress)
                    .addOnSuccessListener(aVoid -> dialog.dismiss())
                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal menyimpan", Toast.LENGTH_SHORT).show());
        });

        // Saat dialog ditutup, hapus referensinya agar tidak menyebabkan memory leak
        dialog.setOnDismissListener(dialogInterface -> currentFormBinding = null);

        dialog.show();
    }
}