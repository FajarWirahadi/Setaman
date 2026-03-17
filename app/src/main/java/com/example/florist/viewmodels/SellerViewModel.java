package com.example.florist.viewmodels;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.R;

public class SellerViewModel extends ViewModel {

    MutableLiveData<Integer> mButtonMutableData = new MutableLiveData<>();

    public SellerViewModel() {
        mButtonMutableData.postValue(R.drawable.rounded_gray_button);
    }

//    public class GenericTextWatcher implements TextWatcher {
//        private View view;
//        private GenericTextWatcher(View view) {
//            this.view = view;
//        }
//
//        @Override
//        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//        }
//
//        @Override
//        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//        }
//
//        @Override
//        public void afterTextChanged(Editable editable) {
//            String text = editable.toString();
//            if (view.getId() == id) {
//                if (text.length() >= 8) {
////                        editTextPhoneNumber.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this ,R.drawable.rounded_success_edittext));
//                    mButtonMutableData.setValue(R.drawable.rounded_success_button);
//                    getButtonResult();
////                        btnRegister.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this, R.drawable.rounded_success_button));
//                } else {
//                    mButtonMutableData.setValue(R.drawable.rounded_gray_button);
//                    getButtonResult();
////                        btnRegister.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this, R.drawable.rounded_gray_button));
////                        editTextPhoneNumber.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this ,R.drawable.rounded_normal_edittext));
//
//                }
//            }
//
//        }
//    }


    public void validateSellerData(View view, int a) {
        Log.d("Here!", "Here");
        boolean isValid;
        class GenericTextWatcher implements TextWatcher {
//            private View view;
//            private int a;
//            private GenericTextWatcher(View view, int a) {
//                this.view = view;
//                this.a= a;
//            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                mButtonMutableData.setValue(R.drawable.rounded_success_button);

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                if (view.getId() == a) {
                    if (text.length() >= 8) {
//                        editTextPhoneNumber.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this ,R.drawable.rounded_success_edittext));
                        mButtonMutableData.setValue(R.drawable.rounded_success_button);
//                        getButtonResult();
//                        btnRegister.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this, R.drawable.rounded_success_button));
                    } else {
                        mButtonMutableData.setValue(R.drawable.rounded_gray_button);
//                        getButtonResult();
//                        btnRegister.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this, R.drawable.rounded_gray_button));
//                        editTextPhoneNumber.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this ,R.drawable.rounded_normal_edittext));

                    }
                }

            }
        }

//        if (!shopName.isEmpty() && !shopUsername.isEmpty()){
//            mButtonMutableData.setValue(R.color.bg_accent);
//        }
    }

    public LiveData<Integer> getButtonResult() {
        return mButtonMutableData;
    }
}
