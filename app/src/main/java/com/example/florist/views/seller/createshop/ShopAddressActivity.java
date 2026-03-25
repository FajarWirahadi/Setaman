package com.example.florist.views.seller.createshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.florist.R;
import com.example.florist.databinding.ActivityShopAddressBinding;
import com.example.florist.viewmodels.ShopViewModel;
import com.example.florist.views.seller.OwnerDashboardActivity;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.geojson.Point;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;

public class ShopAddressActivity extends AppCompatActivity {

    private ActivityShopAddressBinding binding;
    private MapboxMap mapboxMap;
    private PointAnnotationManager pointAnnotationManager;
    private ShopViewModel shopViewModel;
    private String passedShopName;
    private String passedOwnerName;
    private double finalLat = 0.0;
    private double finalLng = 0.0;

    // Menyimpan data dari MapsActivity
    private final ActivityResultLauncher<Intent> mapsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    finalLat = result.getData().getDoubleExtra("LATITUDE", 0.0);
                    finalLng = result.getData().getDoubleExtra("LONGITUDE", 0.0);
                    String address = result.getData().getStringExtra("ADDRESS");
                    String city = result.getData().getStringExtra("CITY");

                    binding.etAddressDetail.setText(address);

                    updateMapPreview(finalLat, finalLng);
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityShopAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        shopViewModel = new ViewModelProvider(this).get(ShopViewModel.class);

        passedShopName = getIntent().getStringExtra("SHOP_NAME");
        passedOwnerName = getIntent().getStringExtra("OWNER_NAME");

        if (passedShopName != null) {
            binding.etShopName.setText(passedShopName);
        }


        setupToolbar();
        setupMapbox();
        setupListeners();
        setupObservers();


    }

    private void setupObservers() {
        shopViewModel.getIsLoading().observe(this, isLoading -> {
            // Tampilkan loading jika ada progress bar (di XML kamu belum ada progress bar loading utamanya)
            binding.btnSaveAddress.setEnabled(!isLoading);
            binding.btnSaveAddress.setText(isLoading ? "Menyimpan..." : "Simpan Alamat");
        });

        shopViewModel.getIsSuccess().observe(this, isSuccess -> {
            if (isSuccess) {
                Toast.makeText(this, "Toko Berhasil Dibuat!", Toast.LENGTH_SHORT).show();
                // Arahkan ke Dashboard Owner
                 Intent intent = new Intent(ShopAddressActivity.this, OwnerDashboardActivity.class);
                 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                 startActivity(intent);
                finish();
            }
        });

        shopViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        binding.btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(ShopAddressActivity.this, MapsActivity.class);
            mapsLauncher.launch(intent);
        });

        binding.btnSaveAddress.setOnClickListener(v -> {
            String finalShopName = binding.etShopName.getText().toString().trim();
            String phone = binding.editTextPhoneNumber.getText().toString().trim();
            String address = binding.etAddressDetail.getText().toString().trim();

            if (finalShopName.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show();
                return;
            }

            if (finalLat == 0.0 || finalLng == 0.0) {
                Toast.makeText(this, "Mohon pilih lokasi di peta", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri defaultImageUri = null;

            shopViewModel.createShop(finalShopName, address, defaultImageUri);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbarTitle.setText(R.string.lokasi);
        binding.btnBack.setOnClickListener(view -> finish());
    }

    private void setupMapbox() {
        mapboxMap = binding.mapViewPreview.getMapboxMap();
        // Load Style dulu
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
            // Setelah style siap, baru kita inisialisasi Marker Manager
            AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(binding.mapViewPreview);
            pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, new AnnotationConfig());

            // JIKA data lokasi sudah ada (misal dari MapsActivity), langsung update tampilan
            if (finalLat != 0.0 && finalLng != 0.0) {
                updateMapPreview(finalLat, finalLng);
            }
        });
    }
    private void updateMapPreview(double finalLat, double finalLng) {
        // Simpan ke variabel global agar aman
        this.finalLat = finalLat;
        this.finalLng = finalLng;

        // Cek apakah Mapbox sudah siap
        if (mapboxMap == null) return;

        // Munculkan CardView
        binding.cardMapPreview.setVisibility(View.VISIBLE);

        // PERHATIKAN: Longitude dulu, baru Latitude!
        Point point = Point.fromLngLat(finalLng, finalLat);

        // Pindahkan Kamera
        mapboxMap.setCamera(new CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build());

        // Tambah Marker
        if (pointAnnotationManager != null) {
            addMarker(point);
        }

        }

    private void addMarker(Point point) {
        // Hapus marker lama
        pointAnnotationManager.deleteAll();

        // --- GANTI BAGIAN INI ---
        // Jangan pakai BitmapFactory.decodeResource(...) untuk file XML!

        // Gunakan fungsi helper yang baru kita buat:
        Bitmap bitmap = bitmapFromDrawableRes(this, R.drawable.location);
        // Pastikan nama file xml-nya sesuai (misal: ic_custom_pin)

        if (bitmap != null) {
            PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(bitmap)
                    .withIconAnchor(IconAnchor.BOTTOM)
                    .withIconSize(0.35);
            // Anchor BOTTOM agar ujung lancip pin tepat di titik lokasi

            pointAnnotationManager.create(pointAnnotationOptions);
        }
    }

    private Bitmap bitmapFromDrawableRes(Context context, int resourceId) {
        Drawable drawable = AppCompatResources.getDrawable(context, resourceId);

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        if (drawable == null) return null;

        // Buat Bitmap kosong sesuai ukuran drawable
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );

        // Gambar Vector ke dalam Bitmap tersebut
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}