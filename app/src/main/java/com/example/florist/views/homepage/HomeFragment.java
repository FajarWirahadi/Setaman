package com.example.florist.views.homepage;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.R;
import com.example.florist.adapter.BuyerProductAdapter;
import com.example.florist.adapter.ShopSuggestionAdapter;
import com.example.florist.databinding.FragmentHomeBinding;
import com.example.florist.model.Notification;
import com.example.florist.model.Product;
import com.example.florist.repository.ProductRepository;
import com.example.florist.viewmodels.CartViewModel;
import com.example.florist.viewmodels.ChatViewModel;
import com.example.florist.viewmodels.NotificationViewModel;
import com.example.florist.viewmodels.ProductViewModel;
import com.example.florist.views.buyer.BuyerDetailActivity;
import com.example.florist.views.buyer.CartActivity;
import com.example.florist.views.chat.InboxActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ProductViewModel productViewModel;
    private CartViewModel cartViewModel;
    private ChatViewModel chatViewModel;
    private NotificationViewModel notificationViewModel;

    private BuyerProductAdapter mainAdapter;
    private BuyerProductAdapter categoryAdapter;
    private ShopSuggestionAdapter shopSuggestionAdapter;
    private Dialog exitDialog;

    private final HashMap<String, ProductRepository.ShopData>  shopDataMap = new HashMap<>();
    private List<Product> allActiveProducts = new ArrayList<>();
    private List<Product> allPlantList = new ArrayList<>();
    private List<Product> outdoorPlantList = new ArrayList<>();
    private List<Product> indoorPlantList = new ArrayList<>();
    private List<Product> ornamentalPlantList = new ArrayList<>();
    private List<Product> tablePlantList = new ArrayList<>();


    private FirebaseFirestore firestore;

    public HomeFragment() {
    }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });

        setupRecyclerView();
        setupSearch();
        setupUI();
        setupViewModel();


        cartViewModel.loadCartCount();
    }

    private void setupViewModel() {
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        cartViewModel.getCartBadgeCount().observe(getViewLifecycleOwner(), this::updateCartBadgeUI);

        productViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.rvCategoryProducts.setVisibility(isLoading ? View.GONE : View.VISIBLE);
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

        productViewModel.getShopDataMap().observe(getViewLifecycleOwner(), map -> {
            if (map != null) {
                shopDataMap.clear();
                shopDataMap.putAll(map);
            }
        });

        notificationViewModel.loadMyNotifications();
        notificationViewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            int unreadCount = 0;

            if (notifications != null) {
                for (Notification notif : notifications) {
                    if (!notif.isRead()) {
                        unreadCount++;
                    }
                }
            }
            updateNotifBadgeUI(unreadCount);
        });

        productViewModel.fetchAllProducts();
        productViewModel.fetchShopNames();
        chatViewModel.loadTotalUnreadCount();
        chatViewModel.getTotalUnreadCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                updateInboxBadgeUI(count);
            }
        });
    }

    private void setupUI() {
        binding.btnCart.setOnClickListener(v -> startActivity(new Intent(requireContext(), CartActivity.class)));
        binding.btnInbox.setOnClickListener(v -> startActivity(new Intent(requireContext(), InboxActivity.class)));
        binding.btnNotification.setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationActivity.class)));
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

        shopSuggestionAdapter = new ShopSuggestionAdapter(shopId -> {
            Intent intent = new Intent(requireContext(), ShopProfileActivity.class);
            intent.putExtra("EXTRA_SHOP_ID", shopId);
            startActivity(intent);
        });
        binding.rvShopSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvShopSuggestions.setAdapter(shopSuggestionAdapter);
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

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                binding.etSearch.clearFocus();
                return true;
            }
            return false;
        });
    }

    private void filterProducts(String query) {
        if (query.isEmpty()) {
            binding.rvShopSuggestions.setVisibility(View.GONE);

            binding.layoutEmptySearch.setVisibility(View.GONE);
            binding.tvPalingPopuler.setVisibility(View.VISIBLE);
            binding.rvBuyerProducts.setVisibility(View.VISIBLE);
            binding.rvCategoryProducts.setVisibility(View.VISIBLE);

            mainAdapter.updateList(allActiveProducts);

            int selectedTabPosition = binding.tabLayout.getSelectedTabPosition();
            if (selectedTabPosition >= 0) {
                String currentCategory = binding.tabLayout.getTabAt(selectedTabPosition).getText().toString();
                filterByCategory(currentCategory);
            }
            return;
        }

        String lowerCaseQuery = query.toLowerCase().trim();

        List<Product> filteredProducts = new ArrayList<>();
        for (Product p : allActiveProducts) {
            boolean isNameMatch = p.getName() != null && p.getName().toLowerCase().contains(lowerCaseQuery);

            // Ambil ShopData, lalu ekstrak namanya
            com.example.florist.repository.ProductRepository.ShopData data = shopDataMap.get(p.getOwnerId());
            String shopName = data != null ? data.shopName : null;

            boolean isShopMatch = shopName != null && shopName.toLowerCase().contains(lowerCaseQuery);

            if (isNameMatch || isShopMatch) {
                filteredProducts.add(p);
            }
        }
        mainAdapter.updateList(filteredProducts);
        if (categoryAdapter != null) {
            categoryAdapter.updateList(filteredProducts);
        }

        List<ShopSuggestionAdapter.ShopItem> matchedShops = new ArrayList<>();
        for (java.util.Map.Entry<String, com.example.florist.repository.ProductRepository.ShopData> entry : shopDataMap.entrySet()) {

            String currentShopName = entry.getValue().shopName;
            String currentShopImg = entry.getValue().shopImageUrl;

            if (currentShopName.toLowerCase().contains(lowerCaseQuery)) {

                matchedShops.add(new ShopSuggestionAdapter.ShopItem(entry.getKey(), currentShopName, currentShopImg));
            }
        }
        if (!matchedShops.isEmpty()) {
            binding.rvShopSuggestions.setVisibility(View.VISIBLE);
            shopSuggestionAdapter.updateList(matchedShops);
        } else {
            binding.rvShopSuggestions.setVisibility(View.GONE);
        }

        boolean isProductEmpty = filteredProducts.isEmpty();
        boolean isShopEmpty = matchedShops.isEmpty();

        if (isProductEmpty && isShopEmpty) {
            binding.layoutEmptySearch.setVisibility(View.VISIBLE);
            binding.tvPalingPopuler.setVisibility(View.GONE);
            binding.rvBuyerProducts.setVisibility(View.GONE);
            binding.rvCategoryProducts.setVisibility(View.GONE);
        } else {
            binding.layoutEmptySearch.setVisibility(View.GONE);
            binding.tvPalingPopuler.setVisibility(View.VISIBLE);
            binding.rvBuyerProducts.setVisibility(View.VISIBLE);
            binding.rvCategoryProducts.setVisibility(View.VISIBLE);
        }
    }
    private void updateCartBadgeUI(int count) {
        if (count > 0) {
            binding.tvCartBadgeCount.setVisibility(View.VISIBLE);
            binding.tvCartBadgeCount.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            binding.tvCartBadgeCount.setVisibility(View.GONE);
        }
    }

    private void updateInboxBadgeUI(int unreadCount) {
        if (unreadCount > 0) {
            binding.tvInboxBadgeCount.setVisibility(View.VISIBLE);
            binding.tvInboxBadgeCount.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        } else {
            binding.tvInboxBadgeCount.setVisibility(View.GONE);
        }
    }

    private void showExitConfirmationDialog() {
        if (exitDialog != null && exitDialog.isShowing()) {
            return;
        }


        exitDialog = com.example.florist.utils.DialogHelper.createCustomDialog(requireContext(), R.layout.dialog_exit_confirmation);
        exitDialog.setCancelable(false);

        MaterialButton btnCancel = exitDialog.findViewById(R.id.btnCancelExit);
        MaterialButton btnConfirm = exitDialog.findViewById(R.id.btnConfirmExit);

        btnCancel.setOnClickListener(v -> exitDialog.dismiss());
        btnConfirm.setOnClickListener(v -> requireActivity().finishAffinity());

        exitDialog.show();
    }
    private void updateNotifBadgeUI(int unreadCount) {
        if (unreadCount > 0) {
            binding.tvNotifBadgeCount.setVisibility(View.VISIBLE);
            binding.tvNotifBadgeCount.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        } else {
            binding.tvNotifBadgeCount.setVisibility(View.GONE);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}