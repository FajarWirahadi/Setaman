package com.example.florist.viewmodels;

public class RegisterFormState {
    private final boolean isNameValid;
    private final boolean isEmailValid;
    private final boolean hasMinLength;
    private final boolean hasUppercase;
    private final boolean hasNumber;
    private final boolean isConfirmValid;
    private final boolean isDataValid;

    public RegisterFormState(boolean isNameValid, boolean isEmailValid, boolean hasMinLength, boolean hasUppercase, boolean hasNumber, boolean isConfirmValid, boolean isDataValid) {
        this.isNameValid = isNameValid;
        this.isEmailValid = isEmailValid;
        this.hasMinLength = hasMinLength;
        this.hasUppercase = hasUppercase;
        this.hasNumber = hasNumber;
        this.isConfirmValid = isConfirmValid;
        this.isDataValid = isDataValid;
    }

    public boolean isNameValid() { return isNameValid; }
    public boolean isEmailValid() { return isEmailValid; }
    public boolean hasMinLength() { return hasMinLength; }
    public boolean hasUppercase() { return hasUppercase; }
    public boolean hasNumber() { return hasNumber; }
    public boolean isConfirmValid() { return isConfirmValid; }
    public boolean isDataValid() { return isDataValid; }
}