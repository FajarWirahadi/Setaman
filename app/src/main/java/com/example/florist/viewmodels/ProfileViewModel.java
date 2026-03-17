package com.example.florist.viewmodels;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.AuthRepository;
import com.example.florist.model.Shop;
import com.example.florist.model.ShopRepository;
import com.example.florist.model.User;
import com.google.firebase.auth.FirebaseUser;

public class ProfileViewModel extends ViewModel {
    private ShopRepository shopRepository;
    private AuthRepository authRepository;
    private MutableLiveData<User> userProfile = new MutableLiveData<User>();
    private MutableLiveData<Shop> shopProfile = new MutableLiveData<Shop>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> navigateToOnboarding = new MutableLiveData<>();
    private MutableLiveData<String> verificationMsg = new MutableLiveData<>();


    public ProfileViewModel() {
        authRepository = AuthRepository.getInstance();
        shopRepository = new ShopRepository();
    }

    public LiveData<User> getUserProfile() {
        return userProfile;
    }
    public LiveData<Shop> getShopProfile() { return shopProfile;}

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<String> getVerificationMsg() { return verificationMsg;}

    public MutableLiveData<Boolean> getNavigateToOnboarding() {
        return navigateToOnboarding;
    }

    public void loadUserProfile() {
        isLoading.setValue(true);
        FirebaseUser firebaseUser = authRepository.getCurrentUser();

        if(firebaseUser != null) {
            authRepository.getUserData(firebaseUser.getUid(), new AuthRepository.UserDataCallback() {
                @Override
                public void onDataLoaded(User user) {
                    userProfile.setValue(user);
                    if (user.isHasShop() && user.getShopId()!= null) {
                        loadShopData(user.getShopId());
                    } else {
                        isLoading.setValue(false);
                    }
                }

                @Override
                public void onError(String e) {
                    isLoading.setValue(false);
                    errorMessage.setValue(e);
                }
            });
        } else {
            isLoading.setValue(false);
            navigateToOnboarding.setValue(true);
            errorMessage.setValue("User tidak ditemukan/belum login");
        }

    }

    private void loadShopData(String shopId) {
        shopRepository.getShopData(shopId, new ShopRepository.ShopDataCallback() {
            @Override
            public void onDataLoaded(Shop shop) {
                isLoading.setValue(false);
                shopProfile.setValue(shop);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void sendVerificationEmail() {
        isLoading.setValue(true);
        authRepository.sendEmailVerification(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                verificationMsg.setValue("Email verifikasi telah dikirim. Silakan cek inbox/spam Anda.");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    // Panggil ini jika user menekan tombol "Saya sudah verifikasi" (Refresh status)
    public void refreshUserStatus() {
        isLoading.setValue(true);
        authRepository.reloadUser(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                if (user.isEmailVerified()) {
                    verificationMsg.setValue("Akun Anda kini sudah terverifikasi!");
                    loadUserProfile();
                } else {
                    errorMessage.setValue("Email belum terverifikasi. Cek email Anda lagi.");
                }
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Data profil tidak ditemukan. Silakan login ulang.");
            }
        });
    }

    public void logout(Context context){
        authRepository.logout(context, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                navigateToOnboarding.setValue(true);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue("Gagal logout: " + message);
            }
        });
    }
}
