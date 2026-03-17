package com.example.florist.views.seller.createshop;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.florist.R;

public class CreateShopSuccess extends AppCompatActivity {

    Button btnLogin;
    TextView tvTitle, tvDescription;
    LinearLayout layoutGif;
    RelativeLayout rlTitle;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_success);
        btnLogin = findViewById(R.id.btnLogin);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        layoutGif = findViewById(R.id.layoutGif);
        rlTitle = findViewById(R.id.rlTitle);

        tvTitle.setText("Toko Berhasil Dibuat");
        tvTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvTitle.setPadding(0,0,0,32);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)rlTitle.getLayoutParams();
        params.addRule(RelativeLayout.ABOVE, R.id.layoutGif);

        tvDescription.setVisibility(View.GONE);

//        RelativeLayout.LayoutParams paramsGif = (RelativeLayout.LayoutParams)layoutGif.getLayoutParams();
//        paramsGif.addRule(RelativeLayout.CENTER_IN_PARENT);
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ffffff")));
//        actionBar.setElevation(0);
    }
}
