package com.example.florist;

import android.app.Application;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;


public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Konfigurasi Cloudinary
        Map<String, String> config = new HashMap<>();


        config.put("cloud_name", "dhkjds1yu");
            config.put("api_key", "323741875367468");
        config.put("api_secret", "LT1RcQQGoP2EEncCuK0aj_aL5EE");

        MediaManager.init(this, config);

    }
}