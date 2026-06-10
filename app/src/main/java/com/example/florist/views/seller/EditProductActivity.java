package com.example.florist.views.seller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.florist.R;
import com.example.florist.adapter.MediaAdapter;
import com.example.florist.databinding.ActivityEditProductBinding;
import com.example.florist.model.Product;
import com.example.florist.utils.DurationBottomSheetFragment;
import com.example.florist.utils.InputCounterHelper;
import com.example.florist.viewmodels.MediaViewModel;
import com.example.florist.viewmodels.ProductFormViewModel;
import com.example.florist.viewmodels.ProductViewModel;
import com.example.florist.views.seller.addproduct.MediaPreviewActivity;
import com.example.florist.views.seller.addproduct.SelectCategoryActivity;

import java.util.ArrayList;
import java.util.List;

public class EditProductActivity extends AppCompatActivity {
    private final int MAX_SELECTION = 5;
    private ActivityEditProductBinding binding;
    private ProductViewModel productViewModel;
    private MediaViewModel mediaViewModel;
    private ProductFormViewModel formViewModel;
    private MediaAdapter adapter;
    private String currentCategoryId = "";
    private String currentCategoryName = "";
    private Product currentProduct;
    private Uri newImageUri = null;

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkInputValidity(); }
        @Override public void afterTextChanged(Editable s) {}
    };

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTION), result -> {
                if (result != null && !result.isEmpty()) {
                    // Update ke ViewModel (UI Media)
                    mediaViewModel.updateMediaList(result);
                    Toast.makeText(this, result.size() + " foto terpilih", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });
    private final ActivityResultLauncher<Intent> categoryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                // Cek apakah hasilnya OK (User memilih, bukan menekan tombol Back)
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        // Ambil data yang dikirim dari halaman sebelah
                        String name = data.getStringExtra("CATEGORY_NAME");
                        String id = data.getStringExtra("CATEGORY_ID");

                        // Update UI
                        binding.tvSelectedCategory.setText(name);
                        binding.tvSelectedCategory.setTextColor(getResources().getColor(R.color.black)); // Biar kelihatan tegas

                        // Simpan ID untuk keperluan upload/database
                        currentCategoryId = id;
                        checkInputValidity();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentProduct = (Product) getIntent().getSerializableExtra("EXTRA_PRODUCT");

        if (currentProduct == null) {
            Toast.makeText(this, "Data produk error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        mediaViewModel = new ViewModelProvider(this).get(MediaViewModel.class);
        formViewModel = new ViewModelProvider(this).get(ProductFormViewModel.class);

        InputCounterHelper.setup(binding.etProductName, binding.tvCountProductName, 70);
        InputCounterHelper.setup(binding.etProductDesc, binding.tvCountProductDesc, 200);

        setupToolbar();
        setupRecyclerView();
        setupInitialData();
        setupInputWatcher();
        setupObservers();
        setupListeners();


        checkInputValidity();
    }

    private void setupObservers() {
        mediaViewModel.getSelectedMedia().observe(this, uris -> {
            if (uris != null) {
                adapter.appendMediaList(uris);
                checkInputValidity();
            }
        });

        formViewModel.getDurationData().observe(this, pair -> {
            if (pair != null) {
                String text = pair.first +  " " + pair.second;
                binding.tvSelectedDuration.setText(text);
                binding.tvSelectedDuration.setTextColor(getColor(R.color.black));
                checkInputValidity();
            }
        });

        formViewModel.getScheduleData().observe(this, scheduleText -> {
            if (scheduleText != null) {
                binding.tvSelectedSchedule.setText(scheduleText);
                binding.tvSelectedSchedule.setTextColor(getColor(R.color.black));
                checkInputValidity();
            }
        });

        productViewModel.getIsLoading().observe(this, isLoading -> {
            binding.btnSaveProduct.setEnabled(!isLoading);
            binding.btnSaveProduct.setText(isLoading ? "Menyimpan..." : "Simpan Produk");
        });

        productViewModel.getIsSuccess().observe(this, isSuccess -> {
            Toast.makeText(this, "Produk berhasil diperbarui", Toast.LENGTH_SHORT).show();
            finish();
        });

        productViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        });

    }

    private void setupRecyclerView() {
        binding.rvMedia.setLayoutManager(new GridLayoutManager(this, 5));

        // Perhatikan parameter tipe data di callback adapter sekarang Object
        adapter = new MediaAdapter(this, MAX_SELECTION, new MediaAdapter.OnItemClickListener() {
            @Override
            public void onAddClick() {
                // Logic: Hitung sisa slot
                int currentSize = adapter.getItemCount() - 1; // dikurang tombol add
                int remaining = MAX_SELECTION - currentSize;

                if (remaining > 0) {
                    pickMedia.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                } else {
                    Toast.makeText(EditProductActivity.this, "Maksimal 5 foto", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteClick(Object mediaItem) {
                mediaViewModel.removeMedia(mediaItem);
            }

            @Override
            public void onPreviewClick(Object mediaItem) {
                Intent intent = new Intent(EditProductActivity.this, MediaPreviewActivity.class);
                intent.putExtra(MediaPreviewActivity.EXTRA_URI, mediaItem.toString());
                startActivity(intent);
            }
        });
        binding.rvMedia.setAdapter(adapter);
    }

    private void openMediaPicker() {
        ActivityResultContracts.PickVisualMedia.VisualMediaType mediaType =
                (ActivityResultContracts.PickVisualMedia.VisualMediaType) ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE;

        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(mediaType)
                .build();

        pickMedia.launch(request);
    }

    private void setupInputWatcher() {
        binding.etProductName.addTextChangedListener(textWatcher);
        binding.etProductDesc.addTextChangedListener(textWatcher);
        binding.etProductPrice.addTextChangedListener(textWatcher);
        binding.etShippingPrice.addTextChangedListener(textWatcher);
        binding.etProductStock.addTextChangedListener(textWatcher);
    }

    private void setupListeners() {
        binding.btnSelectCategory.setOnClickListener(v -> {
            // Buka halaman pilih kategori menggunakan Launcher
            Intent intent = new Intent(EditProductActivity.this, SelectCategoryActivity.class);
            categoryLauncher.launch(intent);
        });

        binding.btnMinimumOrder.setOnClickListener(v -> {
            DurationBottomSheetFragment bottomSheet = DurationBottomSheetFragment.newInstance(DurationBottomSheetFragment.TYPE_DURATION);
            bottomSheet.show(getSupportFragmentManager(), "SheetDuration");
        });

        binding.btnSchedule.setOnClickListener(v -> {
            DurationBottomSheetFragment bottomSheet = DurationBottomSheetFragment.newInstance(DurationBottomSheetFragment.TYPE_SCHEDULE);
            bottomSheet.show(getSupportFragmentManager(),"SheetSchedule");
        });

        binding.btnSaveProduct.setOnClickListener(v -> {updateProduct();});
    }

    private void updateProduct() {
        String name = binding.etProductName.getText().toString().trim();
        String description = binding.etProductDesc.getText().toString().trim();
        double price = 0;
        int stock = 0;
        double shipping = 0;
        try {
            price = Double.parseDouble(binding.etProductPrice.getText().toString().trim());
            stock = Integer.parseInt(binding.etProductStock.getText().toString().trim());
            shipping = Double.parseDouble(binding.etShippingPrice.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Format angka tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = binding.tvSelectedCategory.getText().toString();
        String duration = binding.tvSelectedDuration.getText().toString();
        String schedule = binding.tvSelectedSchedule.getText().toString();

        currentProduct.setName(name);
        currentProduct.setDescription(description);
        currentProduct.setPrice(price);
        currentProduct.setStock(stock);
        currentProduct.setShipping(shipping);
        currentProduct.setCategory(category);
        currentProduct.setDuration(duration);
        currentProduct.setSchedule(schedule);

        List<Object> allMedia = mediaViewModel.getSelectedMedia().getValue();
        List<Uri> newImagesToUpload = new ArrayList<>();
        List<String> oldImagesToKeep = new ArrayList<>();

        if (allMedia != null) {
            for (Object item : allMedia) {
                if (item instanceof Uri) {
                    // Gambar baru dari galeri, masukkan ke dalam antrian upload ke firebase
                    newImagesToUpload.add((Uri) item);
                } else if (item instanceof String){
                    // Gambar lama berupa URL -> masukkan ke list untuk dipertahankan
                    oldImagesToKeep.add((String) item);
                }
            }
        }

        if (newImagesToUpload.isEmpty() && oldImagesToKeep.isEmpty()) {
            Toast.makeText(this, "Minimal sertakan 1 foto produk", Toast.LENGTH_SHORT).show();
            return;
        }
        productViewModel.updateProductMultiple(currentProduct, newImagesToUpload, oldImagesToKeep);
    }

    private void setupInitialData() {
        binding.etProductName.setText(currentProduct.getName());
        binding.etProductDesc.setText(currentProduct.getDescription());
        binding.etProductPrice.setText(String.valueOf((int)currentProduct.getPrice()));
        binding.etProductStock.setText(String.valueOf((int)currentProduct.getStock()));
        binding.etShippingPrice.setText(String.valueOf((int)currentProduct.getShipping()));

        binding.tvSelectedCategory.setText(currentProduct.getCategory());
        binding.tvSelectedDuration.setText(currentProduct.getDuration());
        binding.tvSelectedSchedule.setText(currentProduct.getSchedule());

        currentCategoryName = currentProduct.getCategory();
        currentCategoryId = "OLD_ID";

        List<Object> existingMedia = new ArrayList<>();

        if(currentProduct.getGallery() != null && !currentProduct.getGallery().isEmpty()) {
            existingMedia.addAll(currentProduct.getGallery());
        }
        // Jika tidak punya gallery, tapi punya 1 imageUrl utama
        else if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
            existingMedia.add(currentProduct.getImageUrl());
        }
        // Lempar ke ViewModel agar Adapter memunculkannya di layar
        if (!existingMedia.isEmpty()) {
            mediaViewModel.updateMediaList(existingMedia);
        }
        checkInputValidity();

    }

    private void setupToolbar() {
        binding.toolbarTitle.setText("Ubah Data Produk");
        binding.btnBack.setOnClickListener(v -> {finish();});
        
    }

    private boolean hasText(EditText editText) {
        return editText != null && !editText.getText().toString().trim().isEmpty();
    }

    private void checkInputValidity() {
        boolean areFieldsFilled = hasText(binding.etProductName)
                && hasText(binding.etProductDesc)
                && hasText(binding.etProductPrice)
                && hasText(binding.etProductStock)
                && hasText(binding.etShippingPrice);

        boolean isMediaValid = false;
        if (mediaViewModel.getSelectedMedia().getValue() != null) {
            isMediaValid = !mediaViewModel.getSelectedMedia().getValue().isEmpty();
        }

        boolean isCategoryValid = !currentCategoryId.isEmpty();
        boolean isDurationValid = !binding.tvSelectedDuration.getText().toString().equals("Pilih Durasi");
        boolean isValid = areFieldsFilled && isMediaValid && isCategoryValid && isDurationValid;

        updateSaveButtonStete(isValid);
    }

    private void updateSaveButtonStete(boolean isValid) {
        binding.btnSaveProduct.setEnabled(isValid);
    }
}