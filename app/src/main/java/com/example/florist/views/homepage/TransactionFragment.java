package com.example.florist.views.homepage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.florist.adapter.TransactionPagerAdapter;
import com.example.florist.databinding.FragmentTransactionBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class TransactionFragment extends Fragment {

    private FragmentTransactionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TransactionPagerAdapter pagerAdapter = new TransactionPagerAdapter(requireActivity());
        binding.viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Belanja");
            } else {
                tab.setText("Sewa & Perawatan");
            }
        }).attach();
    }

    public void switchToRentalTab() {
        if (binding != null && binding.viewPager != null) {
            binding.viewPager.setCurrentItem(1, true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}