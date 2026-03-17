package com.example.florist.views.seller.addproduct;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ActivityMediaPreviewBinding;

public class MediaPreviewActivity extends AppCompatActivity {
    private ActivityMediaPreviewBinding binding;
    public static final String EXTRA_URI = "extra_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Ambil data URI dari Intent
        String uriString = getIntent().getStringExtra(EXTRA_URI);
        if (uriString != null) {
            Uri mediaUri = Uri.parse(uriString);
            String mimeType = getContentResolver().getType(mediaUri);

            // Logika Cek Tipe File
            if (mimeType != null && mimeType.contains("video")) {
                // TAMPILKAN VIDEO
                binding.ivPreview.setVisibility(View.GONE);
                binding.vvPreview.setVisibility(View.VISIBLE);

                binding.vvPreview.setVideoURI(mediaUri);

                // Tambahkan kontrol (play/pause)
                MediaController mediaController = new MediaController(this);
                binding.vvPreview.setMediaController(mediaController);
                mediaController.setAnchorView(binding.vvPreview);

                binding.vvPreview.start(); // Auto play

            } else {
                // TAMPILKAN GAMBAR
                binding.vvPreview.setVisibility(View.GONE);
                binding.ivPreview.setVisibility(View.VISIBLE);

                Glide.with(this)
                        .load(mediaUri)
                        .into(binding.ivPreview);
            }
        }

        binding.btnClose.setOnClickListener(v -> finish());
    }
}