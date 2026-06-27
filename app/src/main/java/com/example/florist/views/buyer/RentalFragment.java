package com.example.florist.views.buyer;

import android.content.Intent;
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

import com.example.florist.adapter.RentalAdapter;
import com.example.florist.databinding.FragmentRentalBinding;
import com.example.florist.viewmodels.RentalViewModel;
import com.example.florist.views.homepage.HomepageActivity;

public class RentalFragment extends Fragment {

    private FragmentRentalBinding binding;
    private RentalViewModel viewModel;
    private RentalAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRentalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new RentalAdapter(requireContext());
        viewModel = new ViewModelProvider(this).get(RentalViewModel.class);

        binding.rvRentals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRentals.setAdapter(adapter);


        binding.btnRentNow.setOnClickListener(v -> {
            Intent intent = new android.content.Intent(requireContext(), HomepageActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        setupObservers();

        viewModel.loadMyRentals();
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getRentals().observe(getViewLifecycleOwner(), rentals -> {
            if (rentals != null && !rentals.isEmpty()) {
                adapter.updateData(rentals);
                binding.rvRentals.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.rvRentals.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getActionSuccessMessage().observe(getViewLifecycleOwner(), successMsg -> {
            if (successMsg != null && !successMsg.isEmpty()) {
                Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show();
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