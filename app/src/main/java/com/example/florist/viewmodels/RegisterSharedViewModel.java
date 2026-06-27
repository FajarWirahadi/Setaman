package com.example.florist.viewmodels;

import android.app.Activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.User;
import com.example.florist.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class RegisterSharedViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<User> _userData = new MutableLiveData<>(new User());
    public LiveData<User> getUserData() { return _userData; }

    // State UI
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() { return _isLoading; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    // State Navigasi & Auth
    private final MutableLiveData<Boolean> _isPhoneAvailable = new MutableLiveData<>();
    public LiveData<Boolean> getIsPhoneAvailable() { return _isPhoneAvailable; }

    private final MutableLiveData<Boolean> _isEmailAvailable = new MutableLiveData<>();
    public LiveData<Boolean> getIsEmailAvailable() { return _isEmailAvailable; }

    private final MutableLiveData<String> _verificationId = new MutableLiveData<>();
    public LiveData<String> getVerificationId() { return _verificationId; }

    private final MutableLiveData<FirebaseUser> _registerSuccess = new MutableLiveData<>();
    public LiveData<FirebaseUser> getRegisterSuccess() { return _registerSuccess; }

    public RegisterSharedViewModel() {
        // Mengambil instance singleton dari repository-mu
        authRepository = AuthRepository.getInstance();
    }

    // Langkah 1: Simpan Nomor HP & Cek Ketersediaan
    public void setPhoneNumberAndCheck(String phone) {
        _isLoading.setValue(true);
        User currentUser = _userData.getValue();
        if (currentUser != null) {
            currentUser.setPhoneNumber(phone);
            _userData.setValue(currentUser);
        }

        authRepository.checkPhoneNumberAvailability(phone, new AuthRepository.CheckPhoneAvailability() {
            @Override
            public void onResult(boolean isAvailable) {
                _isLoading.setValue(false);
                _isPhoneAvailable.setValue(isAvailable);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // Langkah 2: Simpan Profil & Cek Ketersediaan Email
    public void setProfileAndCheckEmail(String name, String email, String password) {
        _isLoading.setValue(true);
        User currentUser = _userData.getValue();
        if (currentUser != null) {
            currentUser.setUsername(name);
            currentUser.setEmail(email);
            currentUser.setPassword(password); // Disimpan sementara di ViewModel, bukan di Intent
            _userData.setValue(currentUser);
        }

        authRepository.checkEmailAvailability(email, new AuthRepository.CheckEmailAvailability() {
            @Override
            public void onResult(boolean isAvailable) {
                _isLoading.setValue(false);
                _isEmailAvailable.setValue(isAvailable);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // Langkah 3A: Kirim OTP (Membutuhkan Activity Context untuk reCAPTCHA Firebase)
    public void sendOtp(Activity activity) {
        User currentUser = _userData.getValue();
        if (currentUser == null || currentUser.getPhoneNumber() == null) {
            _errorMessage.setValue("Data nomor HP hilang. Silakan ulangi pendaftaran.");
            return;
        }

        _isLoading.setValue(true);
        authRepository.sendOtp(currentUser.getPhoneNumber(), activity, new AuthRepository.OtpCallback() {
            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                _isLoading.setValue(false);
                _verificationId.setValue(verificationId);
            }

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // Auto-retrieval SMS sukses
                _isLoading.setValue(false);
                // Logika auto-login bisa di-handle di sini jika diperlukan nanti
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }


    public void executeFinalRegistration() {
        _isLoading.setValue(true);
        User currentUser = _userData.getValue();

        if (currentUser == null) {
            _isLoading.setValue(false);
            _errorMessage.setValue("Data pendaftaran tidak valid.");
            return;
        }

        authRepository.register(currentUser, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                _isLoading.setValue(false);
                _registerSuccess.setValue(user);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void resetPhoneAvailability() {
        _isPhoneAvailable.setValue(null);
    }

    // Utility untuk membersihkan pesan error dari View
    public void clearErrors() {
        _errorMessage.setValue(null);
    }
}