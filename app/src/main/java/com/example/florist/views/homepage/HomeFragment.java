package com.example.florist.views.homepage;

import android.content.Intent;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.BuyerProductAdapter;
import com.example.florist.databinding.FragmentHomeBinding;
import com.example.florist.model.Product;
import com.example.florist.viewmodels.CartViewModel;
import com.example.florist.viewmodels.ProductViewModel;
import com.example.florist.views.buyer.BuyerDetailActivity;
import com.example.florist.views.buyer.CartActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ProductViewModel productViewModel;
    private CartViewModel cartViewModel;

    private BuyerProductAdapter mainAdapter;
    private BuyerProductAdapter categoryAdapter;
    private List<Product> allActiveProducts = new ArrayList<>();
    private List<Product> allPlantList = new ArrayList<>();
    private List<Product> outdoorPlantList = new ArrayList<>();
    private List<Product> indoorPlantList = new ArrayList<>();
    private List<Product> ornamentalPlantList = new ArrayList<>();
    private List<Product> tablePlantList = new ArrayList<>();


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
        setupUI();
        setupViewModel();

        cartViewModel.loadCartCount();
    }

    private void setupViewModel() {
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        cartViewModel.getCartBadgeCount().observe(getViewLifecycleOwner(), this::updateCartBadgeUI);

        productViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.svCategoryProducts.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.rvBuyerProducts.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        productViewModel.getAllProducts().observe(getViewLifecycleOwner(), products -> {
            allActiveProducts.clear();
            if (products != null) {
                allActiveProducts.addAll(products);
                mainAdapter.updateList(allActiveProducts);
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0));
                categoryAdapter.updateList(allActiveProducts);
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        productViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), "Gagal: " + error, Toast.LENGTH_SHORT).show();
        });

        productViewModel.fetchAllProducts();
    }

    private void setupUI() {
        binding.btnCart.setOnClickListener(v -> startActivity(new Intent(requireContext(), CartActivity.class)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Semua"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tanaman Indoor"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tanaman Outdoor"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tanaman Ornamental"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tanaman Meja"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Lainnya"));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String selectedCategory = tab.getText().toString();
                filterByCategory(selectedCategory);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void filterByCategory(String selectedCategory) {
        if (selectedCategory.equals("Semua")) {
            categoryAdapter.updateList(allActiveProducts);
            return;
        }

        List<Product> filteredList = new ArrayList<>();
        for (Product p : allActiveProducts) {
            if (p.getCategory() != null && p.getCategory().equalsIgnoreCase(selectedCategory)) {
                filteredList.add(p);
            }
        }
        categoryAdapter.updateList(filteredList);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.rvBuyerProducts.setLayoutManager(layoutManager);

        mainAdapter = new BuyerProductAdapter(requireContext(), new ArrayList<>(),false, product -> {
            Intent intent = new Intent(requireContext(), BuyerDetailActivity.class);
            intent.putExtra("EXTRA_PRODUCT", product);
            startActivity(intent);
        });

        binding.rvBuyerProducts.setAdapter(mainAdapter);

        binding.rvCategoryProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        categoryAdapter = new BuyerProductAdapter(requireContext(), new ArrayList<>(), true, product -> {
            Intent intent = new Intent(requireContext(), BuyerDetailActivity.class);
            intent.putExtra("EXTRA_PRODUCT", product);
            startActivity(intent);
        });
        binding.rvCategoryProducts.setAdapter(categoryAdapter);
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
            if (query.isEmpty()) {
                mainAdapter.updateList(allActiveProducts);

                int selectedTabPosition = binding.tabLayout.getSelectedTabPosition();
                if (selectedTabPosition >= 0) {
                    String currentCategory = binding.tabLayout.getTabAt(selectedTabPosition).getText().toString();
                    filterByCategory(currentCategory);
                }
                return;
            }
            List<Product> filteredList = new ArrayList<>();
            for (Product p : allActiveProducts) {
                if (p.getName() != null && p.getName().toLowerCase().contains(query)) {
                    filteredList.add(p);
                }
            }
            mainAdapter.updateList(filteredList);
            categoryAdapter.updateList(filteredList);
    }

    private void updateCartBadgeUI(int count) {
        if (count > 0) {
            binding.tvCartBadgeCount.setVisibility(View.VISIBLE);
            binding.tvCartBadgeCount.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            binding.tvCartBadgeCount.setVisibility(View.GONE);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}