package com.example.florist.views.homepage;

import android.app.Application;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class MyActivity extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Map config = new HashMap();
        config.put("cloud_name", "dhkjds1yu");
        config.put("api_key", "323741875367468");
//        config.put("api_secret", "**********");

        MediaManager.init(this, config);

        try {
            MediaManager.init(this, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
