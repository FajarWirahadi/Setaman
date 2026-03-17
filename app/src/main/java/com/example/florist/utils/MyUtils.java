package com.example.florist.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.content.res.AppCompatResources;

import com.example.florist.R;
import com.example.florist.views.RegisterPhoneActivity;

public class MyUtils {
    public static class GenericTextWatcher implements TextWatcher {
        public View[] view;
        public int[] id;
        public Context ctx;
        public Button btn;
        boolean isValidA = false;
        boolean isValidB = false;
        public GenericTextWatcher(View[] view, int[] id, Context ctx, Button btn) {
            this.view = view;
            this.id = id;
            this.ctx = ctx;
            this.btn = btn;
        }


        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

            Log.d("here", "here");
            String text = editable.toString();
            if (view[0].getId() == id[0]) {
                //                    editTextPhoneNumber.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this ,R.drawable.rounded_success_edittext));
                //                    btn.setBackground(AppCompatResources.getDrawable(ctx, R.drawable.rounded_success_button));
                //                    btn.setBackground(AppCompatResources.getDrawable(ctx, R.drawable.rounded_gray_button));
                //                    editTextPhoneNumber.setBackground(AppCompatResources.getDrawable(RegisterPhoneActivity.this ,R.drawable.rounded_normal_edittext));
                isValidA = text.length() >= 8;
                Log.d("isValidA = ", String.valueOf(isValidA));
            } else if (view[1].getId() == id[1]){
                isValidB = text.length() >= 8;
                Log.d("isValidB = ", String.valueOf(isValidB));

            }
            if (isValidA && isValidB) {
                btn.setBackground(AppCompatResources.getDrawable(ctx, R.drawable.rounded_success_button));

            }



        }

    }

    public static class MultiTextWatcher {

        public TextWatcherWithInstance callback;

        public MultiTextWatcher setCallback(TextWatcherWithInstance callback) {
            this.callback = callback;
            return this;
        }

        public MultiTextWatcher registerEditText(final EditText editText) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    callback.beforeTextChanged(editText, s, start, count, after);
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    callback.onTextChanged(editText, s, start, before, count);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    callback.afterTextChanged(editText, editable);
                }
            });

            return this;
        }

        public interface TextWatcherWithInstance {
            void beforeTextChanged(EditText editText, CharSequence s, int start, int count, int after);

            void onTextChanged(EditText editText, CharSequence s, int start, int before, int count);

            void afterTextChanged(EditText editText, Editable editable);
        }
    }


}
