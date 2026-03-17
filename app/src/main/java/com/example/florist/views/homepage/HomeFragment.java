package com.example.florist.views.homepage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.GridProductAdapter;
import com.example.florist.databinding.FragmentHomeBinding;
import com.example.florist.model.Product;
import com.example.florist.views.seller.ProductDetailActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore firestore;

    private GridProductAdapter popularAdapter;
    private GridProductAdapter allProductsAdapter;
    private List<Product> fullProductList = new ArrayList<>();

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();

        setupRecyclerViews();
        setupTabs();
        loadProducts();
    }

    private void setupRecyclerViews() {
        popularAdapter = new GridProductAdapter(requireContext(), product -> navigateToDetail(product));
        binding.rvPopular.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvPopular.setAdapter(popularAdapter);

        allProductsAdapter = new GridProductAdapter(requireContext(), product -> navigateToDetail(product));
        binding.rvAllProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvAllProducts.setAdapter(allProductsAdapter);
    }

    private void navigateToDetail(Product product) {
        Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
        intent.putExtra("EXTRA_PRODUCT", product);
        startActivity(intent);
    }

    private void setupTabs() {
        binding.tabLayoutHome.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { filterProducts(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadProducts() {
        firestore.collection("products")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        fullProductList = queryDocumentSnapshots.toObjects(Product.class);
                        if (fullProductList.size() > 5) {
                            popularAdapter.setProductList(fullProductList.subList(0, 5));
                        } else {
                            popularAdapter.setProductList(fullProductList);
                        }
                        allProductsAdapter.setProductList(fullProductList);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void filterProducts(int tabPosition) {
        List<Product> filteredList = new ArrayList<>();
        if (tabPosition == 0) {
            filteredList.addAll(fullProductList);
        } else {
            String keyword = tabPosition == 1 ? "Indoor" : "Outdoor";
            for (Product p : fullProductList) {
                if (p.getCategory() != null && p.getCategory().contains(keyword)) {
                    filteredList.add(p);
                }
            }
        }
        allProductsAdapter.setProductList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
