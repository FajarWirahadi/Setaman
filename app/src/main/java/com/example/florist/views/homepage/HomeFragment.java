package com.example.florist.views.homepage;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.florist.adapter.BuyerProductAdapter;
import com.example.florist.databinding.FragmentHomeBinding;
import com.example.florist.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // 1. Deklarasi ViewBinding
    private FragmentHomeBinding binding;

    private BuyerProductAdapter adapter;
    private List<Product> allActiveProducts = new ArrayList<>();
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();

        setupRecyclerView();
        setupSearch();
        fetchActiveProducts();
    }

    private void setupRecyclerView() {
        binding.rvBuyerProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new BuyerProductAdapter(requireContext(), new ArrayList<>(), product -> {
            Toast.makeText(requireContext(), "Membuka detail: " + product.getName(), Toast.LENGTH_SHORT).show();
        });

        binding.rvBuyerProducts.setAdapter(adapter);
    }

    private void fetchActiveProducts() {
        binding.progressBar.setVisibility(View.VISIBLE);


        firestore.collection("products")
                .whereEqualTo("active", true)
                .whereGreaterThan("stock", 0)
                .addSnapshotListener((value, error) -> {
                    binding.progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(requireContext(), "Gagal memuat etalase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        allActiveProducts = value.toObjects(Product.class);
                        // Tampilkan semua produk ke layar
                        adapter.updateList(allActiveProducts);
                    }
                });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString().toLowerCase().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProducts(String query) {
        List<Product> filteredList = new ArrayList<>();
        for (Product p : allActiveProducts) {
            if (p.getName().toLowerCase().contains(query)) {
                filteredList.add(p);
            }
        }
        adapter.updateList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}