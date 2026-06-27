package com.example.florist.viewmodels;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.User;
import com.example.florist.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential; // Pastikan ini di-import

public class AuthViewModel extends ViewModel {

    private AuthRepository authRepository;
    private MutableLiveData<FirebaseUser> userLiveData;
    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> isLoginFormValid = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isPhoneValid = new MutableLiveData<>(false);
    private MutableLiveData<FirebaseUser> newGoogleUser = new MutableLiveData<>();
    private MutableLiveData<String> otpSentLiveData = new MutableLiveData<>();
    private MutableLiveData<String> emailVerificationMsg = new MutableLiveData<>();
    private final MutableLiveData<RegisterFormState> registerFormState = new MutableLiveData<>();

    // Menyimpan state visibility password
    private final MutableLiveData<Boolean> isPasswordVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isConfirmPasswordVisible = new MutableLiveData<>(false);

    public AuthViewModel() {
        authRepository = AuthRepository.getInstance();
        userLiveData = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
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
    public LiveData<Boolean> getIsLoginFormValid() {
        return isLoginFormValid;
    }
    public LiveData<Boolean> getIsPhoneValid() {
        return isPhoneValid;
    }

    public LiveData<FirebaseUser> getNewGoogleUser() {return newGoogleUser;}

    public LiveData<String> getOtpSentLiveData() {
        return otpSentLiveData;
    }

    public LiveData<String> getEmailVerificationMsg() {return emailVerificationMsg;}
    public LiveData<RegisterFormState> getRegisterFormState() {
        return registerFormState;
    }

    public LiveData<Boolean> getIsPasswordVisible() { return isPasswordVisible; }
    public LiveData<Boolean> getIsConfirmPasswordVisible() { return isConfirmPasswordVisible; }

    public String getCurrentUserId() {
        return authRepository.getCurrentUserId();
    }

    public void togglePasswordVisibility() {
        Boolean current = isPasswordVisible.getValue();
        isPasswordVisible.setValue(current != null ? !current : true);
    }

    public void toggleConfirmPasswordVisibility() {
        Boolean current = isConfirmPasswordVisible.getValue();
        isConfirmPasswordVisible.setValue(current != null ? !current : true);
    }

    public void loginDataChanged(String email, String password) {
        boolean isEmailValid = email != null && !email.trim().isEmpty();
        boolean isPasswordValid = password != null && !password.trim().isEmpty();

        // Tombol hanya valid (true) jika kedua kolom sudah diisi
        isLoginFormValid.setValue(isEmailValid && isPasswordValid);
    }

    public void phoneDataChanged(String phone) {
        boolean isValid = phone != null && !phone.trim().isEmpty() && phone.length() >= 9;
        isPhoneValid.setValue(isValid);
    }
    // --- METODE BARU UNTUK PHONE AUTHENTICATION ---

    public void setLoading(boolean loading) {
        isLoading.setValue(loading);
    }

    public void handleVerificationCompleted(PhoneAuthCredential credential) {
        setLoading(false);
        // Firebase terkadang memverifikasi SMS secara otomatis (auto-retrieval).
        // Untuk saat ini kita matikan logikanya agar flow manual OTP tetap jalan.
    }

    public void handleVerificationFailed(String errorMsg) {
        setLoading(false);
        errorMessage.setValue("Gagal mengirim OTP: " + errorMsg);
    }

    public void handleCodeSent(String verificationId) {
        setLoading(false);
        otpSentLiveData.setValue(verificationId);
    }

    public String formatPhoneNumber(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String trimmed = input.trim();
        if(trimmed.startsWith("0")){
            return "+62" + trimmed.substring(1);
        } else if (trimmed.startsWith("62")) {
            return "+" + trimmed;
        } else if (!trimmed.startsWith("+")) {
            return "+62" + trimmed;
        }
        return trimmed;
    }

    // --- AKHIR METODE BARU ---

    public void login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Email/No HP atau password tidak boleh kosong.");
            return;
        }

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
                userLiveData.setValue(user);
            }

            @Override
            public void onNewUser(FirebaseUser user) {
                User newUser = new User();
                newUser.setUsername(user.getDisplayName() != null ? user.getDisplayName() : "User Baru");
                newUser.setEmail(user.getEmail());
                newUser.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "-");

                saveUserToFirestoreAfterAuth(user.getUid(), newUser);
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

    public void checkEmailAvailability(String email, AuthRepository.CheckEmailAvailability callback) {
        isLoading.setValue(true);

        authRepository.checkEmailAvailability(email, new AuthRepository.CheckEmailAvailability() {
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
                if (user.getPhoneNumber() == null || user.getPhoneNumber().equals("-") || user.getPhoneNumber().isEmpty()) {
                    callback.onError("PROFILE_INCOMPLETE");
                } else {
                    callback.onSuccess(currentUser);
                }
            }

            @Override
            public void onError(String e) {
            }
        });
    }

    public void verifyOtp(String verificationId, String otpCode) {
        isLoading.setValue(true);

        authRepository.verifyOtp(verificationId, otpCode, new AuthRepository.AuthCallback() {
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

    public boolean isUserLoggedIn() {
        return authRepository.getCurrentUser() != null;
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
                            saveUserToFirestoreAfterAuth(user.getUid(), userModel);
                        }

                        @Override
                        public void onError(String message) {
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

    public void refreshUserStatus() {
        isLoading.setValue(true);
        authRepository.reloadUser(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                isLoading.setValue(false);
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

    public FirebaseUser getCurrentUser() {
        if (authRepository == null) {
            authRepository = AuthRepository.getInstance();
        }
        return authRepository.getCurrentUser();
    }

    public void registerDataChanged(String username, String email, String password, String confirmPassword) {
        boolean isNameValid = username != null && !username.trim().isEmpty();
        boolean isEmailValid = email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();

        boolean hasMinLength = password != null && password.length() >= 8;
        boolean hasUppercase = password != null && password.matches(".*[A-Z].*");
        boolean hasNumber = password != null && password.matches(".*[0-9].*");

        boolean isPasswordValid = hasMinLength && hasUppercase && hasNumber;
        boolean isConfirmValid = confirmPassword != null && !confirmPassword.isEmpty() && confirmPassword.equals(password);

        boolean isDataValid = isNameValid && isEmailValid && isPasswordValid && isConfirmValid;

        // Pancarkan state baru ke View
        registerFormState.setValue(new RegisterFormState(
                isNameValid, isEmailValid, hasMinLength, hasUppercase, hasNumber, isConfirmValid, isDataValid
        ));
    }

    public void logout() {
        if (authRepository == null) {
            authRepository = AuthRepository.getInstance();
        }

        authRepository.logout(new AuthRepository.AuthCallback() {
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
}