package com.example.florist.views.seller.addproduct;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.example.florist.databinding.ActivityAddProductBinding;
import com.example.florist.model.Product;
import com.example.florist.utils.DurationBottomSheetFragment;
import com.example.florist.utils.InputCounterHelper;
import com.example.florist.viewmodels.MediaViewModel;
import com.example.florist.viewmodels.ProductFormViewModel;
import com.example.florist.viewmodels.ProductViewModel;

import java.util.List;


public class AddProductActivity extends AppCompatActivity {

    private ActivityAddProductBinding binding;
    private MediaAdapter adapter;
    private MediaViewModel viewModel;
    private ProductViewModel productViewModel;
    private final int MAX_SELECTION = 8;
    private String currentCategoryId = "";
    private String currentCategoryName = "";
    private String currentDuration = "";
    private String currentSchedule = "";

    private ProductFormViewModel formViewModel;

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            checkInputValidity();
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTION), result -> {
                if (result != null && !result.isEmpty()) {
                    viewModel.updateMediaList(result);

                    Toast.makeText(this, result.size() + " media baru dipilih", Toast.LENGTH_SHORT).show();
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
                        currentCategoryName = name;
                        checkInputValidity();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MediaViewModel.class);
        formViewModel = new ViewModelProvider(this).get(ProductFormViewModel.class);
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        InputCounterHelper.setup(binding.etProductName, binding.tvCountProductName, 70);
        InputCounterHelper.setup(binding.etProductDesc, binding.tvCountProductDesc, 200);

        setupToolbar();
        setupRecyclerView();
        setupObserver();
        setupClickListener();
        setupInputWatcher();

        checkInputValidity();

    }

    private void setupToolbar() {
        binding.toolbarTitle.setText("Tambah Produk");
        binding.btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupInputWatcher() {
        binding.etProductName.addTextChangedListener(textWatcher);
        binding.etProductDesc.addTextChangedListener(textWatcher);
        binding.etProductPrice.addTextChangedListener(textWatcher);
        binding.etProductStock.addTextChangedListener(textWatcher);
        binding.etShippingPrice.addTextChangedListener(textWatcher);
    }
    private void setupRecyclerView() {
        binding.rvMedia.setLayoutManager(new GridLayoutManager(this, 5));

        adapter = new MediaAdapter(this, MAX_SELECTION, new MediaAdapter.OnItemClickListener() {
            @Override
            public void onAddClick() {
                openMediaPicker();
            }

            @Override
            public void onDeleteClick(Object uri) {
                viewModel.removeMedia(uri);
            }

            @Override
            public void onPreviewClick(Object uri) {
                Intent intent = new Intent(AddProductActivity.this, MediaPreviewActivity.class);
                intent.putExtra(MediaPreviewActivity.EXTRA_URI, uri.toString());
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


    @SuppressLint("SetTextI18n")
    private void setupObserver() {
        // Ketika data di ViewModel berubah, update Adapter
        viewModel.getSelectedMedia().observe(this, uris -> {
            if (uris != null) {
                // Masukkan data ke Adapter agar muncul di layar
                adapter.appendMediaList(uris);
                checkInputValidity();
            }
        });

        formViewModel.getDurationData().observe(this, pair-> {
            if (pair != null) {
                String amount = pair.first;
                String unit = pair.second;

                // Update UI
                binding.tvSelectedDuration.setText(amount + " " + unit);
                binding.tvSelectedDuration.setTextColor(getResources().getColor(R.color.black));
                checkInputValidity();
            }
        });

        formViewModel.getScheduleData().observe(this, scheduleText-> {
            binding.tvSelectedSchedule.setText(scheduleText);
            Toast.makeText(this, "Jadwal Terpilih" + scheduleText, Toast.LENGTH_SHORT).show();
            checkInputValidity();
        });

        productViewModel.getIsLoading().observe(this, isLoading -> {
            binding.btnSaveProduct.setText(isLoading ? "Menyimpan..." : "Simpan Produk");
            binding.btnSaveProduct.setEnabled(!isLoading);
        });

        productViewModel.getIsSuccess().observe(this, isSuccess -> {
            if (isSuccess) {
                // Pindah ke Halaman Sukses
                Intent intent = new Intent(AddProductActivity.this, ProductPublishedActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Error State
        productViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });


    }

    private void setupClickListener() {
        binding.btnSelectCategory.setOnClickListener(v -> {
            hideKeyboardAndClearFokus();
            // Buka halaman pilih kategori menggunakan Launcher
            Intent intent = new Intent(AddProductActivity.this, SelectCategoryActivity.class);
            categoryLauncher.launch(intent);
        });

        binding.btnMinimumOrder.setOnClickListener(v -> {
            hideKeyboardAndClearFokus();
            DurationBottomSheetFragment bottomSheet = DurationBottomSheetFragment.newInstance(DurationBottomSheetFragment.TYPE_DURATION);
            bottomSheet.show(getSupportFragmentManager(), "SheetDuration");
        });

        binding.btnSchedule.setOnClickListener(v -> {
            hideKeyboardAndClearFokus();
            DurationBottomSheetFragment bottomSheet = DurationBottomSheetFragment.newInstance(DurationBottomSheetFragment.TYPE_SCHEDULE);
            bottomSheet.show(getSupportFragmentManager(),"SheetSchedule");
        });

        binding.btnSaveProduct.setOnClickListener(v -> {
            uploadProduct();
        });

    }

    private void uploadProduct() {
        String name = binding.etProductName.getText().toString().trim();
        String description = binding.etProductDesc.getText().toString().trim();
        String priceStr = binding.etProductPrice.getText().toString().trim();
        String stockStr = binding.etProductStock.getText().toString().trim();
        String shippingStr = binding.etShippingPrice.getText().toString().trim();
        String duration = binding.tvSelectedDuration.getText().toString().trim();
        String schedule = binding.tvSelectedSchedule.getText().toString().trim();

        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);
        double shippingPrice = Double.parseDouble(shippingStr);

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setCategory(currentCategoryName);
        product.setPrice(price);
        product.setStock(stock);
        product.setShipping(shippingPrice);
        product.setDuration(duration);
        product.setSchedule(schedule);

        List<Object> currentMedia = viewModel.getSelectedMedia().getValue();
        Uri coverImageUri = null;
        if (currentMedia != null && !currentMedia.isEmpty()) {
            Object firstItem = currentMedia.get(0);
            // Karena ini Tambah Produk Baru, item pasti berupa Uri
            if (firstItem instanceof Uri) {
                coverImageUri = (Uri) firstItem;
            }
        }


        productViewModel.addProduct(product, coverImageUri);
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
        if (viewModel.getSelectedMedia().getValue() != null) {
            isMediaValid = !viewModel.getSelectedMedia().getValue().isEmpty();
        }

        boolean isCategoryValid = !currentCategoryId.isEmpty();
        boolean isDurationValid = !binding.tvSelectedDuration.getText().toString().equals("Pilih Durasi");
        boolean isValid = areFieldsFilled && isMediaValid && isCategoryValid && isDurationValid;

        updateSaveButtonStete(isValid);
    }

    private void updateSaveButtonStete(boolean isValid) {
        binding.btnSaveProduct.setEnabled(isValid);
    }

    private void hideKeyboardAndClearFokus() {
        View view = this.getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
