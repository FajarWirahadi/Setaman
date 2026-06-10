package com.example.florist.views.seller.addproduct;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.florist.databinding.ActivityProductPublishedBinding;
import com.example.florist.views.seller.OwnerDashboardActivity;

public class ProductPublishedActivity extends AppCompatActivity {

    private ActivityProductPublishedBinding binding;
    private Handler handler;
    private Runnable redirectRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductPublishedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inisialisasi Handler untuk thread utama
        handler = new Handler(Looper.getMainLooper());

        // Definisikan tugas yang akan dijalankan setelah 2 detik
        redirectRunnable = () -> {
            goToMyShop();
        };

        // Jalankan tugas tersebut setelah jeda 2000ms (2 detik)
        handler.postDelayed(redirectRunnable, 2000);

        // OPSIONAL: Jika user tidak sabar dan menekan tombol "Lihat Produk"
        binding.btnSeeProduct.setOnClickListener(v -> {
            // Batalkan timer otomatis agar tidak pindah dua kali
            if (handler != null && redirectRunnable != null) {
                handler.removeCallbacks(redirectRunnable);
            }
            // Langsung pindah
            goToMyShop();
        });
    }

    // Fungsi untuk pindah ke MyShop
    private void goToMyShop() {
        Intent intent = new Intent(ProductPublishedActivity.this, OwnerDashboardActivity.class);
        // Flag ini berguna agar user tidak bisa kembali ke halaman sukses dengan tombol Back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Sangat penting: Hapus callback jika activity dihancurkan sebelum 2 detik
        // Ini mencegah memory leak atau crash
        if (handler != null && redirectRunnable != null) {
            handler.removeCallbacks(redirectRunnable);
        }
    }
}