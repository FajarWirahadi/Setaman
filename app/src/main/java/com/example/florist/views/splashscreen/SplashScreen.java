package com.example.florist.views.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.viewmodels.SplashViewModel;
import com.example.florist.views.LoginActivity;
import com.example.florist.views.chat.ChatRoomActivity;
import com.example.florist.views.homepage.HomepageActivity;
import com.example.florist.views.seller.OwnerDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private SplashViewModel splashViewModel;
    private FirebaseAuth auth;

    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen splashScreen = androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashScreen.setKeepOnScreenCondition(() -> !isDataLoaded);

        auth = FirebaseAuth.getInstance();
        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        setupObservers();

        checkNavigation();
    }

    private void checkNavigation() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            String type = getIntent().getStringExtra("type");

            if ("chat".equals(type)) {
                String targetId = getIntent().getStringExtra("targetId");
                String targetName = getIntent().getStringExtra("targetName");
                String roomId = getIntent().getStringExtra("roomId");

                isDataLoaded = true;

                Intent chatIntent = new Intent(this, ChatRoomActivity.class);
                chatIntent.putExtra("EXTRA_TARGET_ID", targetId);
                chatIntent.putExtra("EXTRA_TARGET_NAME", targetName);
                chatIntent.putExtra("EXTRA_ROOM_ID", roomId);
                chatIntent.putExtra("EXTRA_TARGET_IMAGE", "");

                startActivity(chatIntent);
                finish();
                return;
            }
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            splashViewModel.fetchUserRole(currentUser.getUid());
        } else {
            isDataLoaded = true;
            startActivity(new Intent(SplashScreen.this, LoginActivity.class));
            finish();
        }
    }

    private void setupObservers() {
        splashViewModel.getUserRole().observe(this, role -> {
            isDataLoaded = true;

            Intent intent;
            if ("penjual".equals(role)) {
                intent = new Intent(SplashScreen.this, OwnerDashboardActivity.class);
            } else {
                intent = new Intent(SplashScreen.this, HomepageActivity.class);
            }
            startActivity(intent);
            finish();
        });

        splashViewModel.getErrorMessage().observe(this, error -> {
            isDataLoaded = true;

            Toast.makeText(this, "Gagal memuat data: " + error, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SplashScreen.this, LoginActivity.class));
            finish();
        });
    }
}