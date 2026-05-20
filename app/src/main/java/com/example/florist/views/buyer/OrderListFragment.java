package com.example.florist.views.buyer;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.florist.adapter.OrderAdapter;
import com.example.florist.databinding.FragmentOrderListBinding;
import com.example.florist.viewmodels.OrderViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OrderListFragment extends Fragment {

    private FragmentOrderListBinding binding;
    private OrderViewModel viewModel;
    private OrderAdapter adapter;
    private String currentStatus;
    private String currentUserId;

    public OrderListFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrderListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            currentStatus = getArguments().getString("ORDER_STATUS", "Menunggu Konfirmasi");
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            setupUI();
            setupViewModel();
        } else {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void setupUI() {
        adapter = new OrderAdapter(requireContext());
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {

//             binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getOrderList().observe(getViewLifecycleOwner(), orders -> {
            if (orders == null || orders.isEmpty()) {
                binding.rvOrders.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.rvOrders.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
                adapter.updateData(orders);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
        });

        viewModel.fetchOrders(currentUserId, currentStatus);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}