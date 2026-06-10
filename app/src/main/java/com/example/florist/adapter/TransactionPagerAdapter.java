package com.example.florist.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.florist.views.buyer.RentalFragment;
import com.example.florist.views.buyer.ShoppingFragment;


public class TransactionPagerAdapter extends FragmentStateAdapter {

    public TransactionPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new RentalFragment();
        }
        return new ShoppingFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}