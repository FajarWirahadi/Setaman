package com.example.florist.views.reset_password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.florist.R;
import com.example.florist.views.RegisterActivity;
import com.example.florist.views.RegisterSelectVerificationActivity;

public class NewPasswordActivity extends AppCompatActivity {
    EditText editTextPassword, editTextPassword2,editTextEmail;
    TextView textViewShowPasswordRegister, textViewShowPasswordRegister2, minimumCharPassword, uppercaseCharPassword, minimumNumberPassword;
    ImageView minimumCharPasswordCheck, uppercaseCharPasswordCheck, minimumNumberPasswordCheck;
    Button btnResetPassword;
    public boolean isPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        editTextPassword = findViewById(R.id.editTextPasswordRegister);
        editTextPassword2 = findViewById(R.id.editTextPasswordRegister2);
        textViewShowPasswordRegister = findViewById(R.id.textViewShowPasswordRegister);
        textViewShowPasswordRegister2 = findViewById(R.id.textViewShowPasswordRegister2);
        minimumCharPassword = findViewById(R.id.minimumCharPassword);
        uppercaseCharPassword = findViewById(R.id.uppercaseCharPassword);
        minimumNumberPassword = findViewById(R.id.minimumNumberPassword);
        minimumCharPasswordCheck = findViewById(R.id.minimumCharPasswordCheck);
        uppercaseCharPasswordCheck = findViewById(R.id.uppercaseCharPasswordCheck);
        minimumNumberPasswordCheck = findViewById(R.id.minimumNumberPasswordCheck);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00000000")));

        textViewShowPasswordRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePassVisibility(editTextPassword);
            }
        });
        textViewShowPasswordRegister2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePassVisibility(editTextPassword2);
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int a,b,c,pass;
                checkMinimumCharPassword();
                checkUppercaseCharPassword();
                checkMinimumNumberPassword();
                pass = getColor(R.color.green);
                a = minimumCharPassword.getCurrentTextColor();
                b = uppercaseCharPassword.getCurrentTextColor();
                c = minimumNumberPassword.getCurrentTextColor();
                boolean isSame = checkPassword();
                if (a == pass && b == pass && c == pass){
                    if (isSame) {
//                        Intent intent = new Intent(NewPasswordActivity.this, RegisterSelectVerificationActivity.class);
//                        NewPasswordActivity.this.startActivity(intent);
                    }



                } else {
                    editTextPassword2.setBackground(AppCompatResources.getDrawable(NewPasswordActivity.this, R.drawable.rounded_error_edittext));
                    editTextPassword2.findFocus();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = new Intent(getApplicationContext(), ResetPasswordActivity.class);
        startActivity(intent);
        return true;
    }
    public void togglePassVisibility(EditText editText){
        if (isPasswordVisible) {
            String pass = editText.getText().toString();
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setText(pass);
            editText.setSelection(pass.length());
        }else {
            String pass = editText.getText().toString();
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setText(pass);
            editText.setSelection(pass.length());
        }
        isPasswordVisible = !isPasswordVisible;
    }
    public void checkMinimumCharPassword(){
        if (editTextPassword2.getText().toString().length() <= 8) {
            minimumCharPassword.setTextColor(getColor(R.color.red));
            minimumCharPasswordCheck.setImageResource(R.drawable.error_check);
        } else {
            minimumCharPassword.setTextColor(getColor(R.color.green));
            minimumCharPasswordCheck.setImageResource(R.drawable.success_check);
        }
    }
    public void checkUppercaseCharPassword(){
        String password = editTextPassword2.getText().toString();
        boolean hasUppercase = !password.equals(password.toLowerCase());
        if (!hasUppercase) {
            uppercaseCharPassword.setTextColor(getColor(R.color.red));
            uppercaseCharPasswordCheck.setImageResource(R.drawable.error_check);
        } else {
            uppercaseCharPassword.setTextColor(getColor(R.color.green));
            uppercaseCharPasswordCheck.setImageResource(R.drawable.success_check);
        }
    }
    public void checkMinimumNumberPassword(){
        String password = editTextPassword2.getText().toString();
        if (password.matches("^.*[0-9]+.*$")){
            minimumNumberPassword.setTextColor(getColor(R.color.green));
            minimumNumberPasswordCheck.setImageResource(R.drawable.success_check);
        } else {
            minimumNumberPassword.setTextColor(getColor(R.color.red));
            minimumNumberPasswordCheck.setImageResource(R.drawable.error_check);
        }
    }
    public boolean checkPassword(){
        if (editTextPassword.getText().toString().equals(editTextPassword2.getText().toString())) {
            return true;
        } else {
            editTextPassword2.setBackground(AppCompatResources.getDrawable(NewPasswordActivity.this, R.drawable.rounded_error_edittext));
            editTextPassword.setBackground(AppCompatResources.getDrawable(NewPasswordActivity.this, R.drawable.rounded_error_edittext));
            editTextPassword2.findFocus();
            return false;
        }
    }
}