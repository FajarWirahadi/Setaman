package com.example.florist.views.seller.addproduct;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.R;
import com.example.florist.adapter.CategoryAdapter;
import com.example.florist.model.Category;

import java.util.ArrayList;
import java.util.List;

public class SelectCategoryActivity extends AppCompatActivity {

    private RecyclerView rvAllCategories;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_category);

        rvAllCategories = findViewById(R.id.rvAllCategories);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        rvAllCategories.setLayoutManager(new LinearLayoutManager(this));

        // Data Dummy
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("1", "Tanaman Indoor"));
        categories.add(new Category("2", "Tanaman Outdoor"));
        categories.add(new Category("3", "Tanaman Indoor dan Outdoor"));
        categories.add(new Category("4", "Tanaman Meja"));

        adapter = new CategoryAdapter(this, categories, category -> {
            // --- LOGIKA UTAMA SAAT ITEM DIKLIK ---

            // 1. Siapkan Intent untuk membawa data pulang
            Intent resultIntent = new Intent();
            resultIntent.putExtra("CATEGORY_ID", category.getId());
            resultIntent.putExtra("CATEGORY_NAME", category.getName());

            // 2. Set Result OK (Berhasil)
            setResult(RESULT_OK, resultIntent);

            // 3. Tutup halaman ini
            finish();
        });

        rvAllCategories.setAdapter(adapter);
    }
}