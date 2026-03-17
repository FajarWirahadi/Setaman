package com.example.florist.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.AuthRepository;
import com.example.florist.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class AuthViewModel extends ViewModel {

    private AuthRepository authRepository;

    // Livedata untuk user yang berhasil login
    private MutableLiveData<FirebaseUser> userLiveData;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<FirebaseUser> newGoogleUser = new MutableLiveData<>();
    private MutableLiveData<String> otpSentLiveData = new MutableLiveData<>();
    private MutableLiveData<String> emailVerificationMsg = new MutableLiveData<>();

    public AuthViewModel() {
        authRepository = AuthRepository.getInstance();
        userLiveData = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();

        if (authRepository != null) {
            if (authRepository.getCurrentUser() != null) {
                userLiveData.setValue(authRepository.getCurrentUser());
            }
        }
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<FirebaseUser> getNewGoogleUser() {return newGoogleUser;}

    public LiveData<String> getOtpSentLiveData() {
        return otpSentLiveData;
    }

    public LiveData<String> getEmailVerificationMsg() {return emailVerificationMsg;}

    public void login(String email, String password) {
        if (authRepository == null) {
            authRepository = AuthRepository.getInstance();
        }

        isLoading.setValue(true);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                userLiveData.setValue(user);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void register(User userModel) {
        isLoading.setValue(true);

        authRepository.register(userModel, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                userLiveData.setValue(user);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void loginWithGoogle(String idToken) {
        if (authRepository == null) {
            authRepository = AuthRepository.getInstance();
        }
        isLoading.setValue(true);
        authRepository.firebaseAuthWithGoogle(idToken, new AuthRepository.GoogleAuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                userLiveData.setValue(user);// User lama trigger ke HomepageActivity
            }

            @Override
            public void onNewUser(FirebaseUser user) {
                isLoading.setValue(false);
                newGoogleUser.setValue(user);// User baru trigger ke RegisterActivity
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void checkPhoneAvailability (String phoneNumber, AuthRepository.CheckPhoneAvailability callback) {
        isLoading.setValue(true);

        authRepository.checkPhoneNumberAvailability(phoneNumber, new AuthRepository.CheckPhoneAvailability() {
            @Override
            public void onResult(boolean isAvailable) {
                isLoading.setValue(false);
                callback.onResult(isAvailable);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal mengecek nomor " + message);
                callback.onError(message);
            }
        });
    }

    public void checkEmailAvailability(String email, AuthRepository.CheckEmailAvailabitiy callback) {
        isLoading.setValue(true);

        authRepository.checkEmailAvailability(email, new AuthRepository.CheckEmailAvailabitiy() {
            @Override
            public void onResult(boolean isAvailable) {
                isLoading.setValue(false);
                callback.onResult(isAvailable);
            }
            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal mengecek email " + message);
                callback.onError(message);
            }
        });
    }

    // Fungsi untuk mengecek apakah profil user sudah lengkap
    public void checkIsProfileComplete(AuthRepository.AuthCallback callback) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;
        // Ambil data detail dari Firestore
        authRepository.getUserData(currentUser.getUid(), new AuthRepository.UserDataCallback() {
            @Override
            public void onDataLoaded(User user) {
                // LOGIKA UTAMA: Cek apakah No HP masih kosong atau default "-"
                if (user.getPhoneNumber() == null || user.getPhoneNumber().equals("-") || user.getPhoneNumber().isEmpty()) {
                    // Profil BELUM Lengkap
                    callback.onError("PROFILE_INCOMPLETE");
                } else {
                    // Profil SUDAH Lengkap
                    callback.onSuccess(currentUser);
                }
            }

            @Override
            public void onError(String e) {
                // Handle error ambil data
            }
        });
    }

    public void sendOtp(String phoneNumber, android.app.Activity activity) {
        isLoading.setValue(true);

        authRepository.sendOtp(phoneNumber, activity, new AuthRepository.OtpCallback() {
            @Override
            public void onCondSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                isLoading.setValue(false);
                otpSentLiveData.setValue(verificationId);
            }

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                isLoading.setValue(false);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void verifyOtp(String verificationId, String otpCode) {
        isLoading.setValue(true);

        authRepository.verifyOtp(verificationId, otpCode, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                userLiveData.setValue(user); // Kabari View bahwa verifikasi sukses
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message); // Kabari View ada error (misal kode salah)
            }
        });
    }

    public boolean isUserLoggedIn() {
        return authRepository.getCurrentUser() != null;
    }

    public void logout(Context context) {
        authRepository.logout(context, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                userLiveData.setValue(null);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue("Gagal logout: " + message);
            }
        });
    }

    public void verifyAndRegisterUser(String verificationId, String code, User userModel) {
        isLoading.setValue(true);

        authRepository.verifyOtp(verificationId, code, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (userModel.getEmail() != null && !userModel.getEmail().isEmpty()
                && userModel.getPassword() != null && !userModel.getPassword().isEmpty()) {

                    authRepository.linkEmailAndPassword(userModel.getEmail(), userModel.getPassword(), new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            // Linking sukses simpan ke firebase
                            saveUserToFirestoreAfterAuth(user.getUid(), userModel);
                        }

                        @Override
                        public void onError(String message) {
                            // Lingking gagal tapi OPT sukses
                            isLoading.setValue(false);
                            errorMessage.setValue("Gagal mendaftarkan email: " + message);
                        }
                    });
                } else {
                    saveUserToFirestoreAfterAuth(user.getUid(), userModel);
                }
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("OPT salah " + message);
            }
        });


    }

    public void saveUserToFirestoreAfterAuth(String uid, User user) {
        user.setUserId(uid);
        authRepository.saveUserToFirestore(uid, user, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser u) {
                isLoading.setValue(false);
                userLiveData.setValue(u);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal simpan data " + message);
            }
        }, FirebaseAuth.getInstance().getCurrentUser());
    }

    public void sendVerificationEmail() {
        isLoading.setValue(true);
        authRepository.sendEmailVerification(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                emailVerificationMsg.setValue("Email verifikasi telah dikirim. Silakan cek inbox/spam Anda.");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

//     Panggil ini jika user menekan tombol "Saya sudah verifikasi" (Refresh status)
    public void refreshUserStatus() {
        isLoading.setValue(true);
        authRepository.reloadUser(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
                // Cek apakah sekarang sudah verified
                if (user.isEmailVerified()) {
                    emailVerificationMsg.setValue("Akun Anda kini sudah terverifikasi!");
                    userLiveData.setValue(user);
                } else {
                    errorMessage.setValue("Email belum terverifikasi. Cek email Anda lagi.");
                }
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal memuat status user: " + message);
            }
        });
    }

}