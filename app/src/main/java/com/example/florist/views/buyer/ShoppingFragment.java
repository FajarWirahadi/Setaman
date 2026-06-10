package com.example.florist.views.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.R;
import com.example.florist.adapter.BuyerOrderAdapter;
import com.example.florist.databinding.FragmentShoppingBinding;
import com.example.florist.viewmodels.ShoppingViewModel;
import com.example.florist.views.homepage.TransactionFragment;

public class ShoppingFragment extends Fragment {

    private FragmentShoppingBinding binding;
    private ShoppingViewModel viewModel;
    private BuyerOrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShoppingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inisialisasi ViewModel Pertama Kali
        viewModel = new ViewModelProvider(this).get(ShoppingViewModel.class);

        // 2. Setup UI dan Observers
        setupUI();
        setupObservers();

        // 3. Muat Data
        viewModel.loadMyOrders();
    }

    private void setupUI() {
        // 4. Inisialisasi Adapter dengan Interface Callback MVVM
        adapter = new BuyerOrderAdapter(requireContext(), new BuyerOrderAdapter.OnOrderActionListener() {
            @Override
            public void onAcceptOrder(com.example.florist.model.Order order) {
                if (viewModel != null) viewModel.acceptOrder(order);
            }

            @Override
            public void onEndRental(com.example.florist.model.Order order) {
                if (viewModel != null) viewModel.endRental(order);
            }
        });

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);

        binding.chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            String statusFilter = "SEMUA";

            if (checkedId == R.id.chipBelumBayar) {
                statusFilter = "PENDING";
            } else if (checkedId == R.id.chipDiproses) {
                statusFilter = "PROCESSING";
            } else if (checkedId == R.id.chipDikirim) {
                statusFilter = "SHIPPED";
            } else if (checkedId == R.id.chipSelesai) {
                statusFilter = "COMPLETED";
            }

            viewModel.setFilter(statusFilter);
        });
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getFilteredOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null && !orders.isEmpty()) {
                adapter.updateData(orders);
                binding.rvOrders.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.rvOrders.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        // 5. Observer untuk Aksi Sukses (Terima Pesanan / Akhiri Sewa)
        viewModel.getActionSuccessMessage().observe(getViewLifecycleOwner(), successMsg -> {
            if (successMsg != null && !successMsg.isEmpty()) {
                Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show();

                // Cek apakah fragment induknya adalah TransactionFragment
                if (getParentFragment() instanceof TransactionFragment) {
                    // Perintahkan pindah ke tab "Sewa & Perawatan" otomatis
                    ((TransactionFragment) getParentFragment()).switchToRentalTab();
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}