package com.example.florist.views.homepage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.BuyerProductAdapter;
import com.example.florist.databinding.FragmentShopProductBinding;
import com.example.florist.model.Product;
import com.example.florist.viewmodels.ShopProfileViewModel;
import com.example.florist.views.buyer.BuyerDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class ShopProductFragment extends Fragment {
    private FragmentShopProductBinding binding;
    private BuyerProductAdapter adapter;
    private ShopProfileViewModel viewModel;
    private int tabPosition;

    public static ShopProductFragment newInstance(int position) {
        ShopProductFragment fragment= new ShopProductFragment();
        Bundle args = new Bundle();
        args.putInt("TAB_POSITION", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null ) {
            tabPosition = getArguments().getInt("TAB_POSITION");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShopProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ShopProfileViewModel.class);

        setupRecyclerView();
        setupObservers();
        
    }

    private void setupRecyclerView() {
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new BuyerProductAdapter(requireContext(), new ArrayList<>(), true, product -> {
            Intent intent = new Intent(requireContext(), BuyerDetailActivity.class);

            intent.putExtra("EXTRA_PRODUCT", product);
            startActivity(intent);
        });
        binding.rvProducts.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getShopProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                List<Product> displayList = new ArrayList<>();

                if (tabPosition == 0) {
                    int limit = Math.min(products.size(), 4);
                    for (int i = 0; i < limit; i++) {
                        displayList.add(products.get(i));
                    }
                } else if (tabPosition == 1) {
                    displayList.addAll(products);
                } else {
                    displayList.clear();
                }

                adapter.updateList(displayList);

                if (displayList.isEmpty()) {
                    binding.rvProducts.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.rvProducts.setVisibility(View.VISIBLE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

