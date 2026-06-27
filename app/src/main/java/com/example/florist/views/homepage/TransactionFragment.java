package com.example.florist.views.homepage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.florist.databinding.FragmentTransactionBinding;
import com.example.florist.views.buyer.RentalFragment;

// PENTING: Import kelas Fragment Rental Anda di sini
// Misalnya: import com.example.florist.views.buyer.RentalFragment;

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

        // [ENTERPRISE]: Langsung suntikkan Fragment Sewa & Perawatan tanpa ViewPager
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    // GANTI 'new RentalFragment()' dengan nama kelas Fragment Sewa Anda yang sebenarnya
                    .replace(binding.fragmentContainer.getId(), new RentalFragment())
                    .commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}