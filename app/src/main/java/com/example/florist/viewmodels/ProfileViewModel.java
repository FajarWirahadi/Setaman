package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Shop;
import com.example.florist.model.User;
import com.example.florist.repository.AuthRepository;
import com.example.florist.repository.ShopRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewModel extends ViewModel {
    private ShopRepository shopRepository;
    private AuthRepository authRepository;

    private MutableLiveData<Integer> countUnpaid = new MutableLiveData<>();
    private MutableLiveData<Integer> countProcessing = new MutableLiveData<>();
    private MutableLiveData<Integer> countShipped = new MutableLiveData<>();
    private MutableLiveData<Integer> countRented = new MutableLiveData<>();

    private MutableLiveData<User> userProfile = new MutableLiveData<User>();
    private MutableLiveData<Shop> shopProfile = new MutableLiveData<Shop>();
    private MutableLiveData<Boolean> isUpdateSuccess = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> navigateToOnboarding = new MutableLiveData<>();
    private MutableLiveData<String> verificationMsg = new MutableLiveData<>();


    public ProfileViewModel() {
        authRepository = AuthRepository.getInstance();
        shopRepository = new ShopRepository();
    }

    public LiveData<Integer> getCountUnpaid() {return countUnpaid;}
    public LiveData<Integer> getCountProcessing() {return countProcessing;}
    public LiveData<Integer> getCountShipped() {return countShipped;}
    public LiveData<Integer> getCountRented() {return countRented;}

    public LiveData<User> getUserProfile() {
        return userProfile;
    }
    public LiveData<Shop> getShopProfile() { return shopProfile;}
    public LiveData<Boolean> getIsUpdateSuccess() {return isUpdateSuccess;}
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

        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();

            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        isLoading.setValue(false);

                        if (error != null) {
                            errorMessage.setValue("Gagal mendapatkan informasi akun: " + error.getMessage());
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            userProfile.setValue(user);

                            if (user != null && user.isHasShop() && user.getShopId() != null) {
                                loadShopData(user.getShopId());
                            }
                        }

                    });
        } else {
            isLoading.setValue(false);
            navigateToOnboarding.setValue(true);
            errorMessage.setValue("User tidak ditemukan atau belum login");
        }

    }

    private void loadShopData(String shopId) {
        shopRepository.getShopById(shopId, new ShopRepository.ShopDataCallback() {
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

    public void loadOrderCounts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String buyerId = user.getUid();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("orders")
                .whereEqualTo("buyerId", buyerId)
                .whereEqualTo("status", "MENUNGGU KONFIRMASI")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        countUnpaid.setValue((int) task.getResult().getCount());
                    }
                });

        firestore.collection("orders")
                .whereEqualTo("buyerId", buyerId)
                .whereEqualTo("status","DIPROSES")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        countProcessing.setValue((int) task.getResult().getCount());
                    }
                });

        firestore.collection("orders")
                .whereEqualTo("buyerId", buyerId)
                .whereEqualTo("status", "DIKIRIM")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        countShipped.setValue((int) task.getResult().getCount());
                    }
                });

        firestore.collection("orders")
                .whereEqualTo("buyerId", buyerId)
                .whereEqualTo("status", "SELESAI")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        countRented.setValue((int) task.getResult().getCount());
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

    public void updateProfileImage(android.net.Uri imageUri) {
        isLoading.setValue(true);

        authRepository.updateUserProfileImage(imageUri, new AuthRepository.UpdateProfileCallback() {
            @Override
            public void onSuccess(String newImageUrl) {
                isLoading.setValue(false);
                loadUserProfile();
            }
            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

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

    public void updateProfileData(String newName, String newPhone) {
        isLoading.setValue(true);

        authRepository.updateUserData(newName, newPhone, new AuthRepository.UpdateDataCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                isUpdateSuccess.setValue(true);
                loadUserProfile();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal menyimpan: " + message);
            }
        });
    }
    public void resetUpdateStatus() {
        isUpdateSuccess.setValue(false);
    }

    public void logout() {
        authRepository.logout(new AuthRepository.AuthCallback() {
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
