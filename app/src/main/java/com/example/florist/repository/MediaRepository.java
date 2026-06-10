package com.example.florist.repository;

import android.net.Uri;

import java.util.List;

public class MediaRepository {
    //Logika Retrofit/Upload ke server
    // Untuk sekarang, hanya pass-through data Uri
    public List<Uri> processSelectedMedia(List<Uri> uris) {
        // Simulasi proses data (misal: kompresi, filter, dll)
        return uris;
    }
}
