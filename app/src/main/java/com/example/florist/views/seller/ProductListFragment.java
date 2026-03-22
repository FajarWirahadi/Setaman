package com.example.florist.views.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.R;
import com.example.florist.adapter.ProductAdapter;
import com.example.florist.model.Product;
import com.example.florist.viewmodels.MyProductViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProductListFragment extends Fragment {
    private int tabPosition;
    private ProductAdapter adapter;
    private MyProductViewModel viewModel;
    private View layoutEmptyState;
    private TextView tvEmptyState;
    private RecyclerView rv;

    public static ProductListFragment newInstance(int position) {

        ProductListFragment fragment = new ProductListFragment();
        Bundle args = new Bundle();
        args.putInt("TAB_POSITION", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) tabPosition = getArguments().getInt("TAB_POSITION");

        viewModel = new ViewModelProvider(requireActivity()).get(MyProductViewModel.class);

        tvEmptyState = view.findViewById(R.id.tvEmptyMessage);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        rv = view.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ProductAdapter(getContext(), new ArrayList<>(), new ProductAdapter.OnProductActionClickListener() {
            @Override
            public void onEditClick(Product product) {
                Intent intent = new Intent(getContext(), EditProductActivity.class);
                intent.putExtra("EXTRA_PRODUCT", product);
                startActivity(intent);
            }

            @Override
            public void onDeactivateClick(Product product) {
                boolean newStatus = !product.isActive();
                viewModel.toggleProductStatus(product.getId(), newStatus);
            }

            @Override
            public void onMenuClick(Product product, View view) {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), view);
                popup.getMenu().add("Hapus Produk");

                popup.setOnMenuItemClickListener(item -> {
                    showDeleteConfirmation(product);
                    return true;
                });
                popup.show();
            }
        });
        rv.setAdapter(adapter);

        if (tabPosition == 0) {
            viewModel.getActiveProducts().observe(getViewLifecycleOwner(), this::updateUI);
        } else if (tabPosition ==1) {
            viewModel.getSoldProducts().observe(getViewLifecycleOwner(), this::updateUI);
        } else {
            viewModel.getInactiveProducts().observe(getViewLifecycleOwner(), this::updateUI);
        }

        viewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success!=null && success) {
                Toast.makeText(getContext(), "Produk Berhasil dihapus", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showDeleteConfirmation(Product product) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        bottomSheet.setContentView(R.layout.dialog_delete_confirmation);

        android.widget.TextView tvMessage = bottomSheet.findViewById(R.id.tvDeleteMessage);
        com.google.android.material.button.MaterialButton btnCancel = bottomSheet.findViewById(R.id.btnCancelDelete);
        com.google.android.material.button.MaterialButton btnConfirm = bottomSheet.findViewById(R.id.btnConfirmDelete);

        if (tvMessage != null) {
            tvMessage.setText("Apakah Anda yakin ingin menghapus '" + product.getName() + "'? Tindakan ini permanen dan tidak dapat dibatalkan.");
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> bottomSheet.dismiss());
        }


        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                bottomSheet.dismiss();
                viewModel.deleteProduct(product);
            });
        }
        bottomSheet.show();
    }

    private void updateUI(List<Product> products) {
        if (products == null || products.isEmpty()) {
            rv.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);

            if (tabPosition == 0) {
                tvEmptyState.setText("Belum ada produk yang aktif. Mulai tambahkan produk pertamamu!");
            } else if (tabPosition == 1) {
                tvEmptyState.setText("Kerja bagus! Belum ada produk yang kehabisan stok");
            } else {
                tvEmptyState.setText("Tidak ada produk yang dinonaktifkan");
            }
        } else {
            rv.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);

        }
        adapter.updateList(products);
    }

}
