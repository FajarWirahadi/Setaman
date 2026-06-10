package com.example.florist.utils;

import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

public class InputCounterHelper {

    public static void setup(EditText editText, TextView countTextView, int maxLength) {

        editText.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(maxLength)
        });
        updateCounter(countTextView, editText.length(), maxLength);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                updateCounter(countTextView, charSequence.length(), maxLength);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }


    public static void updateCounter(TextView textView, int currentLength, int maxLength) {
        textView.setText(currentLength + "/" + maxLength);

        if (currentLength ==  maxLength) {
            textView.setTextColor(Color.RED);
        } else {
            textView.setTextColor(Color.GRAY);
        }
    }
}
