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
import com.example.florist.views.buyer.MyOrdersActivity;
import com.example.florist.views.seller.OwnerDashboardActivity;
import com.example.florist.views.seller.createshop.ShopIntroActivity;
import com.example.florist.views.splashscreen.OnboardingActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    private final androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickProfileImage =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    viewModel.updateProfileImage(uri);
                }
            });

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
        viewModel.loadOrderCounts();
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
                if (shop.getShopImageUrl() != null && !shop.getShopImageUrl().isEmpty()) {
                    binding.imgShop.setPadding(0, 0, 0,0);
                    binding.imgShop.clearColorFilter();

                    com.bumptech.glide.Glide.with(requireContext())
                            .load(shop.getShopImageUrl())
                            .placeholder(R.drawable.building)
                            .centerCrop()
                            .into(binding.imgShop);
                } else {
                    binding.imgShop.setPadding(30, 30, 30, 30);
                }
            }
        });

        viewModel.getCountUnpaid().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.badgeUnpaid.setVisibility(View.VISIBLE);
                binding.badgeUnpaid.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.badgeUnpaid.setVisibility(View.GONE);
            }
        });

        viewModel.getCountProcessing().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.badgeProcessing.setVisibility(View.VISIBLE);
                binding.badgeProcessing.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.badgeProcessing.setVisibility(View.GONE);
            }
        });

        viewModel.getCountShipped().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.badgeShipped.setVisibility(View.VISIBLE);
                binding.badgeShipped.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.badgeShipped.setVisibility(View.GONE);
            }
        });

        viewModel.getCountRented().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.badgeRented.setVisibility(View.VISIBLE);
                binding.badgeRented.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.badgeRented.setVisibility(View.GONE);
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
            requireActivity().finish();
        });

        binding.btnMyOrders.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MyOrdersActivity.class));
        });

        binding.imgProfile.setOnClickListener(v -> {
            pickProfileImage.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.myAccount.btnEditProfileAcc.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            intent.putExtra("EXTRA_USERNAME", binding.tvUsername.getText().toString());
            startActivity(intent);
        });

        binding.menuUnpaid.setOnClickListener(v -> openMyOrdersAtTab(0));
        binding.menuProcessing.setOnClickListener(v -> openMyOrdersAtTab(0));
        binding.menuShipped.setOnClickListener(v -> openMyOrdersAtTab(0));
        binding.menuRented.setOnClickListener(v -> openMyOrdersAtTab(0));


    }

    private void openMyOrdersAtTab(int tabIndex) {
        Intent intent = new Intent(requireContext(), MyOrdersActivity.class);
        intent.putExtra("TAB_INDEX", tabIndex);
        startActivity(intent);
    }

    private void updateUi(User user) {
        binding.tvUsername.setText(user.getUsername());
        binding.tvUserId.setText("ID : " + user.getUserId());

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(requireContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.imgProfile);
        }

        if (user.isHasShop()) {
            binding.tvShopName.setText("Memuat data toko...");
            binding.tvShopId.setText("Kelola toko saya");

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
        binding = null;
    }
}