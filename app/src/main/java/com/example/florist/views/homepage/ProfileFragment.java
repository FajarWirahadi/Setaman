package com.example.florist.views.homepage;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.databinding.FragmentProfileBinding;
import com.example.florist.model.User;
import com.example.florist.utils.DialogHelper;
import com.example.florist.viewmodels.ProfileViewModel;
import com.example.florist.views.LoginActivity;
import com.example.florist.views.buyer.MyOrdersActivity;
import com.example.florist.views.buyer.RentalFragment;
import com.example.florist.views.seller.OwnerDashboardActivity;
import com.example.florist.views.seller.createshop.ShopIntroActivity;
import com.example.florist.views.splashscreen.OnboardingActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private GoogleSignInClient googleSignInClient;
    private Dialog logoutDialog;

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

        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireContext(), gso);

        binding.toolbarTitle.setText("Akun Saya");

        setupUI();
        setupObservers();
        setupListeners();

        viewModel.loadUserProfile();
        viewModel.loadOrderCounts();
    }

    private void setupUI() {
        if (binding.menuUnpaid != null) {
            binding.menuUnpaid.setOnClickListener(v -> openMyOrdersWithTab(0)); // Tab "Menunggu"
        }

        if (binding.menuProcessing != null) {
            binding.menuProcessing.setOnClickListener(v -> openMyOrdersWithTab(1)); // Tab "Diproses"
        }

        if (binding.menuShipped != null) {
            binding.menuShipped.setOnClickListener(v -> openMyOrdersWithTab(2)); // Tab "Dikirim"
        }

        if (binding.menuRented != null) {
            binding.menuRented.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new RentalFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (binding.btnMyOrders != null) {
            binding.btnMyOrders.setOnClickListener(v -> openMyOrdersWithTab(0));
        }
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
            showCustomLogoutDialog();
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

    private void openMyOrdersWithTab(int tabIndex) {
        Intent intent = new Intent(requireContext(), MyOrdersActivity.class);
        intent.putExtra("TAB_INDEX", tabIndex);
        startActivity(intent);
    }

    private void logoutAndRedirect() {
        Intent intent = new Intent(requireContext(), OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showCustomLogoutDialog() {
        if (logoutDialog != null && logoutDialog.isShowing()) {
            return;
        }

        logoutDialog = DialogHelper.createCustomDialog(requireContext(), R.layout.dialog_logout_confirmation);

        ImageButton btnClose = logoutDialog.findViewById(R.id.btnCloseDialog);
        MaterialButton btnCancel = logoutDialog.findViewById(R.id.btnCancelLogout);
        MaterialButton btnLogout = logoutDialog.findViewById(R.id.btnConfirmLogout);
        TextView tvDescription = logoutDialog.findViewById(R.id.tvLogoutDescription);

        String fullText = "Banyak koleksi tanaman baru yang siap bikin sudut ruanganmu lebih estetik. " +
                "Sampai jumpa lagi! Jika ingin berganti akun, Anda dapat login menggunakan akun lain.";
        String clickableText = "login menggunakan akun lain.";

        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(clickableText);
        int endIndex = startIndex + clickableText.length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                logoutDialog.dismiss(); // [PERBAIKAN]

                viewModel.logout();
                if (googleSignInClient != null) {
                    googleSignInClient.signOut().addOnCompleteListener(task -> {});
                }

                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(Color.BLACK);
                ds.setFakeBoldText(true);
            }
        };

        if (startIndex != -1) {
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvDescription.setText(spannableString);
        tvDescription.setMovementMethod(LinkMovementMethod.getInstance());

        btnClose.setOnClickListener(v -> logoutDialog.dismiss());
        btnCancel.setOnClickListener(v -> logoutDialog.dismiss());

        btnLogout.setOnClickListener(v -> {
            logoutDialog.dismiss();
            viewModel.logout();

            if (googleSignInClient != null) {
                googleSignInClient.signOut().addOnCompleteListener(task -> {});
            }
        });

        logoutDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}