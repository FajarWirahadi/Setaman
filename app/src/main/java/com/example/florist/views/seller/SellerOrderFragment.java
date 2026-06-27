package com.example.florist.views.seller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.R;
import com.example.florist.adapter.SellerOrderAdapter;
import com.example.florist.databinding.DialogUpdateDeliveryBinding;
import com.example.florist.databinding.FragmentSellerOrderBinding;
import com.example.florist.model.Order;
import com.example.florist.utils.Constants;
import com.example.florist.viewmodels.SellerDeliveryViewModel;
import com.example.florist.viewmodels.SellerOrderViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SellerOrderFragment extends Fragment {
    private FragmentSellerOrderBinding binding;
    private SellerOrderAdapter adapter;
    private SellerOrderViewModel viewModel;
    private SellerDeliveryViewModel deliveryViewModel;
    private String orderStatus;

    public static SellerOrderFragment newInstance(String status) {
        SellerOrderFragment fragment = new SellerOrderFragment();
        Bundle args = new Bundle();
        args.putString("ORDER_STATUS", status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderStatus = getArguments().getString("ORDER_STATUS");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerOrderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SellerOrderViewModel.class);
        deliveryViewModel = new ViewModelProvider(this).get(SellerDeliveryViewModel.class);

        setupRecyclerView();
        setupObservers();

        viewModel.fetchSellerOrders(orderStatus);
    }


    private void setupRecyclerView() {
        adapter = new SellerOrderAdapter(requireContext(), new SellerOrderAdapter.OnOrderActionListener() {
            @Override
            public void onAcceptClicked(Order order) {
                String newStatus = order.getStatus().equals(Constants.ORDER_WAITING)
                        ? Constants.ORDER_PROCESSING
                        : Constants.ORDER_SHIPPED;
                viewModel.updateOrderStatus(order, newStatus);
            }

            @Override
            public void onRejectClicked(Order order) {
                showRejectDialog(order);
            }

            @Override
            public void onUpdateDeliveryClicked(Order order) {
                showUpdateDeliveryDialog(order.getOrderId());
            }
        });


        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);
    }

    private void showUpdateDeliveryDialog(String orderId) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogUpdateDeliveryBinding dialogBinding = DialogUpdateDeliveryBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        dialogBinding.btnSubmitDelivery.setOnClickListener(v -> {
            int selectedId = dialogBinding.rgDeliveryStatus.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(requireContext(), "Silakan pilih tahapan terlebih dahulu!", Toast.LENGTH_SHORT).show();
                return;
            }

            dialogBinding.btnSubmitDelivery.setEnabled(false);
            dialogBinding.btnSubmitDelivery.setText("Menyimpan...");

            RadioButton selectedRadioButton = dialogBinding.getRoot().findViewById(selectedId);
            String statusTitle = selectedRadioButton.getText().toString();
            String note = dialogBinding.etDeliveryNote.getText().toString().trim();

            deliveryViewModel.addDeliveryLog(orderId, statusTitle, note);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showRejectDialog(Order order) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        bottomSheet.setContentView(R.layout.dialog_reject_order);

        RadioGroup radioGroup = bottomSheet.findViewById(R.id.radioGroupReason);
        Button btnConfirmReject = bottomSheet.findViewById(R.id.btnConfirmReject);

        if (btnConfirmReject != null && radioGroup != null) {
            btnConfirmReject.setOnClickListener(v -> {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(requireContext(), "Pilih alasan penolakan terlebih dahulu!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnConfirmReject.setEnabled(false);
                btnConfirmReject.setText("Memproses...");
                RadioButton selectedRadio = bottomSheet.findViewById(selectedId);
                String reason = selectedRadio.getText().toString();

                viewModel.rejectOrder(order, reason);
            });
        }
        bottomSheet.show();
    }

    private void setupObservers() {
        viewModel.getAllSellerOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                adapter.updateData(orders);
                if (orders.isEmpty()) {
                    binding.rvOrders.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.rvOrders.setVisibility(View.VISIBLE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getActionSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                Toast.makeText(requireContext(), success, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        deliveryViewModel.getIsSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess != null && isSuccess) {
                Toast.makeText(requireContext(), "Status pengantaran berhasil diupdate!", Toast.LENGTH_SHORT).show();
                viewModel.fetchSellerOrders(orderStatus);
            }
        });

        deliveryViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
