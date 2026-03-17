package com.example.florist.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.florist.views.seller.ProductListFragment;

public class ProductPagerAdapter extends FragmentStateAdapter {
    public ProductPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return ProductListFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
