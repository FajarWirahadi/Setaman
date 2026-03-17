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
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.FragmentProfileBinding;
import com.example.florist.model.User;
import com.example.florist.viewmodels.ProfileViewModel;
import com.example.florist.views.LoginActivity;
import com.example.florist.views.seller.OwnerDashboardActivity;
import com.example.florist.views.seller.createshop.ShopIntroActivity;
import com.example.florist.views.splashscreen.OnboardingActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    // WAJIB KOSONG. Fragment butuh konstruktor kosong.
    public ProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding.toolbarTitle.setText("Akun Saya");

        setupObservers();
        setupListeners();

        viewModel.loadUserProfile();
    }

    private void setupObservers() {

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateUi(user);
            } else {
                logoutAndRedirect();
            }
        });

        viewModel.getShopProfile().observe(getViewLifecycleOwner(), shop -> {
            if (shop != null) {
                binding.tvShopName.setText(shop.getShopName());
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                binding.loadingOverlay.setVisibility(View.VISIBLE);
            } else {
                binding.loadingOverlay.setVisibility(View.GONE);
            }
        });

        viewModel.getNavigateToOnboarding().observe(getViewLifecycleOwner(), redirect -> {
            if (redirect != null && redirect) {
                logoutAndRedirect();
            }
        });
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            viewModel.logout(requireContext());
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish(); // Menutup Activity yang mewadahi Fragment ini
        });
    }

    private void updateUi(User user) {
        binding.tvUsername.setText(user.getUsername());
        binding.tvUserId.setText("ID : " + user.getUserId());

        if (user.isHasShop()) {
            binding.tvShopName.setText("Memuat data toko...");
            binding.tvShopId.setText("Kelola toko saya");

            // Mengambil warna di Fragment harus via requireContext()
            binding.iconBackground.setBackgroundTintList(requireContext().getColorStateList(R.color.olive_500));
            binding.imgShop.setColorFilter(requireContext().getColor(R.color.white));

            binding.btnJoinPartner.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), OwnerDashboardActivity.class);
                startActivity(intent);
            });
        } else {
            binding.tvShopName.setText("Bergabung menjadi mitra!");
            binding.tvShopId.setText("Buka toko gratis");

            binding.iconBackground.setBackgroundTintList(requireContext().getColorStateList(R.color.gray_100));
            binding.imgShop.clearColorFilter();

            binding.btnJoinPartner.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ShopIntroActivity.class);
                startActivity(intent);
            });
        }
    }

    private void logoutAndRedirect() {
        Intent intent = new Intent(requireContext(), OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // MENCEGAH MEMORY LEAK
        binding = null;
    }
}