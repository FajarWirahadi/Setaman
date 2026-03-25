package com.example.florist.views.seller.createshop;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.florist.R;
import com.example.florist.databinding.ActivityMapsBinding;
import com.example.florist.viewmodels.MapsViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapboxMap;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;
import com.mapbox.search.ResponseInfo;
import com.mapbox.search.SearchEngine;
import com.mapbox.search.SearchEngineSettings;
import com.mapbox.search.SearchOptions;
import com.mapbox.search.SearchSelectionCallback;
import com.mapbox.search.result.SearchResult;
import com.mapbox.search.result.SearchSuggestion;
import com.mapbox.geojson.Point;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity {

    private ActivityMapsBinding binding;
    private MapsViewModel viewModel;
    private MapboxMap mapboxMap;
    private SearchEngine searchEngine;
    private LocationComponentPlugin locationComponentPlugin;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
               if (isGranted) {
                   initLocationComponent();
               } else {
                   Toast.makeText(MapsActivity.this, "Izin lokasi diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show();
               }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MapsViewModel.class);
        mapboxMap = binding.mapView.getMapboxMap();
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
            setupMapGestures();

            locationComponentPlugin = LocationComponentUtils.getLocationComponent(binding.mapView);
        });

        try {
            searchEngine = SearchEngine.createSearchEngine(
                    new SearchEngineSettings(getString(R.string.mapbox_access_token))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = toolbar.findViewById(R.id.toolbar_title);
        toolbar_title.setText(R.string.lokasi_peta);
        setSupportActionBar(toolbar);

        setupObservers();
        setupListener();

    }

    private void initLocationComponent() {
        if (locationComponentPlugin == null) return;

        locationComponentPlugin.setEnabled(true);

        OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = new OnIndicatorPositionChangedListener() {
            @Override
            public void onIndicatorPositionChanged(@NonNull Point point) {
                mapboxMap.setCamera(new CameraOptions.Builder()
                        .center(point)
                        .zoom(17.0)
                        .build());

                // Simpan data ke ViewModel (agar alamat di bawah terupdate)
                fetchAddressFromCoordinates(point);

                // kalau tidak dihapus user bisa geser ke tempat lain.
                locationComponentPlugin.removeOnIndicatorPositionChangedListener(this);
            }
        };
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    }
    private void setupObservers() {
        // 1. Observer untuk NAMA JALAN (tvKnownName)
        viewModel.getAddressName().observe(this, name -> {
            if (name != null && !name.isEmpty()) {
                binding.tvKnownName.setText(name); // <--- INI KUNCINYA
            } else {
                binding.tvKnownName.setText("Nama jalan tidak diketahui");
            }
        });

        // 2. Observer untuk ALAMAT LENGKAP (tvAddress)
        viewModel.getAddressDetail().observe(this, detail -> {
            binding.tvAddress.setText(detail);

            // Aktifkan tombol lanjut jika alamat sudah ketemu
            binding.btnNext.setEnabled(true);
        });
    }

    private void setupListener() {
        binding.btnBack.setOnClickListener(v -> {finish();});

        binding.fabMyLocation.setOnClickListener(v -> {
            checkPermissionAndLocate();
        });
        binding.btnSearchTrigger.setOnClickListener(v -> {showSearchDialog();});
        binding.btnNext.setOnClickListener(v-> {
            Point point = viewModel.getSelectedPoint().getValue();
            String address = viewModel.getAddressDetail().getValue();
            String city = viewModel.getCityName().getValue();
            if (point != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("LATITUDE", point.latitude());
                resultIntent.putExtra("LONGITUDE", point.longitude());
                resultIntent.putExtra("ADDRESS", address);
                resultIntent.putExtra("CITY", city);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void checkPermissionAndLocate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
            initLocationComponent();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void showSearchDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_search);

        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);

            bottomSheet.getLayoutParams().height = WindowManager.LayoutParams.MATCH_PARENT;
        }

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        EditText etQuery = dialog.findViewById(R.id.et_search_query);
        RecyclerView rvResult = dialog.findViewById(R.id.rv_search_results);

        SearchResultAdapter adapter = new SearchResultAdapter(suggestion -> {
           selectSuggestion(suggestion, dialog);
        });

        rvResult.setLayoutManager(new LinearLayoutManager(this));
        rvResult.setAdapter(adapter);

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 2) performSearch(s.toString(), adapter);
            }
        });
        dialog.show();
    }
    private void performSearch(String query, SearchResultAdapter adapter) {
        if (searchEngine == null) return;

        SearchOptions options = new SearchOptions().toBuilder().limit(5).build();

        // search() dengan SearchSuggestionCallback
        searchEngine.search(query, options, new SearchSelectionCallback() {
            @Override
            public void onResult(@NonNull SearchSuggestion searchSuggestion, @NonNull SearchResult searchResult, @NonNull ResponseInfo responseInfo) {

            }

            @Override
            public void onResults(@NonNull SearchSuggestion searchSuggestion, @NonNull List<SearchResult> list, @NonNull ResponseInfo responseInfo) {

            }

            @Override
            public void onSuggestions(@NonNull List<SearchSuggestion> list, @NonNull ResponseInfo responseInfo) {
                adapter.setSuggestions(list);
            }

            @Override
            public void onError(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void selectSuggestion(SearchSuggestion suggestion, BottomSheetDialog dialog) {
        // Mengambil detail lengkap termasuk koordinat
        searchEngine.select(suggestion, new SearchSelectionCallback() {
            @Override
            public void onResult(@NonNull SearchSuggestion searchSuggestion, @NonNull SearchResult searchResult, @NonNull ResponseInfo responseInfo) {
                com.mapbox.geojson.Point point = searchResult.getCoordinate();
                if (point != null) {
                    moveCamera(point);
                    dialog.dismiss();
                }
            }

            @Override
            public void onResults(@NonNull SearchSuggestion searchSuggestion, @NonNull List<SearchResult> list, @NonNull ResponseInfo responseInfo) {

            }

            @Override
            public void onSuggestions(@NonNull List<SearchSuggestion> list, @NonNull ResponseInfo responseInfo) {

            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this, "Gagal mengambil lokasi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMapGestures() {
        GesturesUtils.getGestures(binding.mapView).addOnMoveListener(new OnMoveListener() {
            @Override
            public void onMoveBegin(com.mapbox.android.gestures.MoveGestureDetector detector) {
            }

            @Override
            public boolean onMove(com.mapbox.android.gestures.MoveGestureDetector detector) {
                return false;
            }

            @Override
            public void onMoveEnd(com.mapbox.android.gestures.MoveGestureDetector detector) {
                fetchAddressFromCoordinates(mapboxMap.getCameraState().getCenter());
            }
        });
    }


    private void fetchAddressFromCoordinates(Point point) {
        binding.tvKnownName.setText("Mencari nama jalan...");
        binding.tvAddress.setText("");
        binding.btnNext.setEnabled(false);

        // 1. Jalankan di Thread background karena Geocoder tidak boleh di Main Thread
        new Thread(() -> {
            try {
                // Gunakan Locale Indonesia agar formatnya "Jalan", bukan "Street"
                Geocoder geocoder = new Geocoder(this, new Locale("id", "ID"));

                // Minta max 1 hasil dari koordinat Mapbox
                List<Address> addresses = geocoder.getFromLocation(
                        point.latitude(),
                        point.longitude(),
                        1
                );

                // 2. Proses Hasilnya di UI Thread
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address googleAddress = addresses.get(0);

                        // --- AMBIL DATA DARI GOOGLE ---

                        // A. Nama Jalan (Thoroughfare)
                        String streetName = googleAddress.getThoroughfare();

                        // Tambahkan nomor rumah/gedung jika ada (SubThoroughfare)
                        if (googleAddress.getSubThoroughfare() != null) {
                            streetName += " No. " + googleAddress.getSubThoroughfare();
                        }

                        // Fallback: Jika Google pun tidak nemu nama jalan (misal di hutan)
                        // Ambil nama fitur terdekat atau baris pertama alamat
                        if (streetName == null) {
                            if (googleAddress.getFeatureName() != null) {
                                streetName = googleAddress.getFeatureName();
                            } else {
                                // Ambil potongan depan alamat panjang
                                String line0 = googleAddress.getAddressLine(0);
                                if (line0 != null) streetName = line0.split(",")[0];
                            }
                        }

                        // B. Alamat Lengkap
                        String fullAddress = googleAddress.getAddressLine(0);
                        String city = googleAddress.getSubAdminArea();
                        if (city == null || city.isEmpty()) {
                            city = googleAddress.getLocality();
                        }
                        if (city == null || city.isEmpty()) {
                            city = googleAddress.getAdminArea();
                        }
                        // --- KIRIM KE VIEWMODEL ---
                        viewModel.setLocationDetail(point, streetName, fullAddress,city);

                    } else {
                        binding.tvKnownName.setText("Lokasi tidak dikenal");
                        binding.tvAddress.setText("Coba geser ke area lain");
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Fallback Error: Koneksi internet bermasalah
                    binding.tvKnownName.setText("Gagal memuat");
                    binding.tvAddress.setText("Periksa internet Anda");
                });
            }
        }).start();
    }

    private void moveCamera(com.mapbox.geojson.Point point) {
        mapboxMap.setCamera(new CameraOptions.Builder().center(point).zoom(16.0).build());
        fetchAddressFromCoordinates(point);
    }


}