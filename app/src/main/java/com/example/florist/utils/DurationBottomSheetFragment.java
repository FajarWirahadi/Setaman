package com.example.florist.utils;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.florist.R;
import com.example.florist.viewmodels.ProductFormViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class DurationBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String EXTRA_TYPE = "extra_type";
    public static final String TYPE_DURATION = "type_duration";
    public static final String TYPE_SCHEDULE = "type_schedule";


    private ProductFormViewModel viewModel;
    private LinearLayout layoutDuration, layoutSchedule;
    private TextView tvTitle;
    private String currentType;
    private EditText etAmount;
    private RadioGroup rgUnit, rgSchedule;
    private Button btnSave, btnClose;

    // Method Static Agar Activity mudah dipanggil
    public static DurationBottomSheetFragment newInstance(String type) {
        DurationBottomSheetFragment fragment = new DurationBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_duration_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- KODE BARU (FIX) ---
        // Kita tambahkan parameter kedua: getDefaultViewModelProviderFactory()
        // Ini memberi tahu ViewModelProvider cara membuat ViewModel tersebut.

        if (getArguments() != null) {
            currentType = getArguments().getString(EXTRA_TYPE, TYPE_DURATION);
        }
        viewModel = new ViewModelProvider(requireActivity(), requireActivity().getDefaultViewModelProviderFactory()).get(ProductFormViewModel.class);

        initViews(view);
        setupUIBasedOnType();
        restoreSavedData();
        setupListener();

//        if (btnClose != null) {
//            btnClose.setOnClickListener(v -> {
//                dismiss();
//            });
//        }
//
//        btnSave.setOnClickListener(v ->{
//            String amount = etAmount.getText().toString();
//            if(amount.isEmpty()) {
//                Toast.makeText(getContext(), "Masukkan jumlah durasi", Toast.LENGTH_SHORT).show();
//            }
//
//            int selectedId = rgUnit.getCheckedRadioButtonId();
//            RadioButton selectedRb = view.findViewById(selectedId);
//            String unit = selectedRb.getText().toString();
//            btnSave.setTextColor(getResources().getColor(R.color.bg_success));
//            viewModel.setDuration(amount, unit);
//            dismiss();
//        });
    }

    private void initViews(View view) {
        layoutDuration = view.findViewById(R.id.layout_input_duration);
        layoutSchedule = view.findViewById(R.id.layout_input_schedule);
        tvTitle = view.findViewById(R.id.tv_sheet_title);
        etAmount = view.findViewById(R.id.et_duration_amount);
        rgUnit = view.findViewById(R.id.rg_duration_unit);
        rgSchedule = view.findViewById(R.id.rg_schedule_options);
        btnSave = view.findViewById(R.id.btn_save_generic);

        view.findViewById(R.id.btn_close_sheet).setOnClickListener(v ->{
            dismiss();
        });
    }

    private void setupUIBasedOnType () {
        layoutDuration.setVisibility(View.GONE);
        layoutSchedule.setVisibility(View.GONE);
        btnSave.setEnabled(false);

        if (TYPE_DURATION.equals(currentType)) {
            tvTitle.setText("Minimal Durasi Sewa");
            layoutDuration.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("Jadwal Perawatan");
            layoutSchedule.setVisibility(View.VISIBLE);
        }
    }

    private void setupListener() {
        TextWatcher durationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {checkInputValidity();}

            @Override
            public void afterTextChanged(Editable editable) {
                checkInputValidity();
            }
        };

        etAmount.addTextChangedListener(durationWatcher);

        rgUnit.setOnCheckedChangeListener(((group, checkedId) -> {checkInputValidity();}));

        rgSchedule.setOnCheckedChangeListener(((g, i) -> {checkInputValidity();}));

        btnSave.setOnClickListener(v -> {saveData();});

    }
    private void checkInputValidity() {
        boolean isValid = false;

        if (TYPE_DURATION.equals(currentType)) {
            boolean amountFilled = !etAmount.getText().toString().isEmpty();
            boolean unitSelected = rgUnit.getCheckedRadioButtonId() != -1;
            isValid = amountFilled && unitSelected;
        } else {
            isValid = rgSchedule.getCheckedRadioButtonId() != -1;
        }

        btnSave.setEnabled(isValid);

//        if (isAmountFilled && isUnitSelected) {
//            btnSave.setEnabled(true);
//            btnSave.setBackgroundResource(R.drawable.rounded_success_button);
//        } else {
//            btnSave.setEnabled(false);
//            btnSave.setBackgroundResource(R.drawable.rounded_gray_button);
//        }
    }

    private void saveData() {
        if (TYPE_DURATION.equals(currentType)) {
            String amount = etAmount.getText().toString();
            RadioButton rb = getView().findViewById(rgUnit.getCheckedRadioButtonId());
            viewModel.setDuration(amount, rb.getText().toString());
        } else {
            RadioButton rb = getView().findViewById(rgSchedule.getCheckedRadioButtonId());
            viewModel.setSchedule(rb.getText().toString());
        }
        dismiss();
    }

    private void restoreSavedData() {
        if(TYPE_DURATION.equals(currentType)) {
            viewModel.getDurationData().observe(getViewLifecycleOwner(), pair ->{
                if(pair != null) {
                    etAmount.setText(pair.first);

                    // Pilih RadioButton yang sesuai (cth: "Minggu")
                    setRadioButtonByText(rgUnit, pair.second);
                }

            });
        } else {
            viewModel.getScheduleData().observe(getViewLifecycleOwner(), schedule -> {
                if (schedule != null) {
                    setRadioButtonByText(rgSchedule, schedule);
                }
            });
        }
    }

    private void setRadioButtonByText(RadioGroup group, String textToFind) {
        if (textToFind == null) return;

        for (int i=0; i<group.getChildCount(); i++){
            View child = group.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton rb = (RadioButton) child;
                if (rb.getText().toString().equalsIgnoreCase(textToFind)) {
                    rb.setChecked(true);
                    return;
                }
            }
        }
    }
}