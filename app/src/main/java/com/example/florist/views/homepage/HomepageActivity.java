package com.example.florist.views.homepage;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.florist.R;
import com.example.florist.databinding.ActivityHomepageBinding;
import com.example.florist.views.chat.ChatRoomActivity;

public class HomepageActivity extends AppCompatActivity {

    private ActivityHomepageBinding binding;
    private String pendingOrderIdForTransaction = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomepageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
        if (savedInstanceState == null) {

            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }

        setupToolbar();
        setupBottomNav();
        checkNotificationRouting();
    }

    private void setupToolbar() {
    }

    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_trans) {
                selectedFragment = new TransactionFragment();

                // --- INJEKSI BUNDLE KE FRAGMENT ---
                if (pendingOrderIdForTransaction != null) {
                    Bundle args = new Bundle();
                    args.putString("EXTRA_ORDER_ID", pendingOrderIdForTransaction);
                    selectedFragment.setArguments(args);

                    pendingOrderIdForTransaction = null;
                }
                // ----------------------------------

            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void checkNotificationRouting() {
        if (getIntent() != null && getIntent().getExtras() != null) {

            String type = getIntent().getStringExtra("type");
            String navigateTo = getIntent().getStringExtra("navigate_to");

            if ("chat".equals(type)) {
                String targetId = getIntent().getStringExtra("targetId");
                String targetName = getIntent().getStringExtra("targetName");

                Intent chatIntent = new Intent(this, ChatRoomActivity.class);
                chatIntent.putExtra("EXTRA_TARGET_ID", targetId);
                chatIntent.putExtra("EXTRA_TARGET_NAME", targetName);
                chatIntent.putExtra("EXTRA_TARGET_IMAGE", "");
                startActivity(chatIntent);

            } else if ("new_order".equals(type)) {
                Intent orderIntent = new Intent(this, com.example.florist.views.seller.OwnerDashboardActivity.class);
                orderIntent.putExtra("EXTRA_ORDER_ID", getIntent().getStringExtra("orderId"));
                startActivity(orderIntent);

            } else if ("product".equals(type)) {
                Intent productIntent = new Intent(this, com.example.florist.views.buyer.BuyerDetailActivity.class);
                productIntent.putExtra("EXTRA_PRODUCT_ID", getIntent().getStringExtra("productId"));
                startActivity(productIntent);

            } else if ("delivery_update".equals(type) || "transaction_tab".equals(navigateTo)) {
                binding.bottomNav.setSelectedItemId(R.id.nav_trans);
            }
        }
    }
}