package com.example.florist.repository;

import android.app.Activity;
import android.net.Uri;
import android.util.Patterns;

import androidx.annotation.NonNull;

import com.example.florist.model.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class AuthRepository {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private static AuthRepository instance;

    public interface OtpCallback {
        void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token);
        void onVerificationCompleted(PhoneAuthCredential credential);
        void onError(String errorMessage);
    }
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    public interface UserDataCallback {
        void onDataLoaded(User user);
        void onError(String message);
    }

    public interface CheckPhoneAvailability {
        void onResult(boolean isAvailable);
        void onError(String message);
    }

    public interface GoogleAuthCallback {
        void onSuccess(FirebaseUser user);
        void onNewUser(FirebaseUser user);
        void onError(String message);
    }
    public interface CheckEmailAvailability {
        void onResult(boolean isAvailable);
        void onError(String message);
    }

    public interface UpdateProfileCallback {
        void onSuccess(String newImageUrl);
        void onError(String message);
    }

    public interface UpdateDataCallback {
        void onSuccess();
        void onError(String message);
    }

    private AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public String getCurrentUserId() {
        if (firebaseAuth.getCurrentUser() != null) {
            return firebaseAuth.getCurrentUser().getUid();
        }
        return null;
    }

    public void checkPhoneNumberAvailability(String phoneNumber, CheckPhoneAvailability callback) {
        String formattedPhone = formatPhoneNumber(phoneNumber);

        firestore.collection("users")
                .whereEqualTo("phoneNumber", formattedPhone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isAvailable = task.getResult().isEmpty();
                        callback.onResult(isAvailable);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void checkEmailAvailability(String email, CheckEmailAvailability callback) {
        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())    {
                        boolean isAvailable = task.getResult().isEmpty();
                        callback.onResult(isAvailable);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void login(String input, String password, AuthCallback callback) {
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            performFirebaseLogin(input, password, callback);
        } else {
            String formattedPhone = formatPhoneNumber(input);

            firestore.collection("users")
                    .whereEqualTo("phoneNumber", formattedPhone)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            QuerySnapshot snapshots = task.getResult();
                            if (!snapshots.isEmpty() && snapshots != null) {
                                User user = snapshots.getDocuments().get(0).toObject(User.class);

                                if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                                    performFirebaseLogin(user.getEmail(), password, callback);
                                } else {
                                    callback.onError("Akun ini tidak memliliki email yang terhubung.");
                                }
                            } else {
                                callback.onError("Nomor HP tidak terdaftar");
                            }
                        } else {
                            callback.onError("Gagal mengecek nomor HP: " + task.getException().getMessage());
                        }
                    });
        }
    }

    private void performFirebaseLogin(String input, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(input, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveDeviceToken(firebaseAuth.getCurrentUser().getUid());
                        callback.onSuccess(firebaseAuth.getCurrentUser());
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            callback.onError("Email/No HP yang anda masukkan tidak terdaftar");
                        } else if (e instanceof  FirebaseAuthInvalidCredentialsException) {
                            callback.onError("Password salah");
                        } else {
                            callback.onError(e != null ? e.getMessage() : "Login gagal");
                        }
                    }
                });
    }

    private String formatPhoneNumber(String input) {
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

    public void register(User userModel, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(userModel.getEmail(), userModel.getPassword())
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful()) {
                       FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                       if (firebaseUser != null) {
                           String userId = firebaseUser.getUid();
                           userModel.setUserId(userId);
                           userModel.setPassword(null);

                           saveUserToFirestore(userId, userModel, callback, firebaseUser);
                       } else {
                           callback.onError(task.getException().getMessage());
                       }
                   }
                });
    }

    public void saveUserToFirestore(String userId, User userModel, AuthCallback callback, FirebaseUser firebaseUser) {
        firestore.collection("users").document(userId)
                .set(userModel)
                .addOnSuccessListener(aVoid -> {
                    saveDeviceToken(userId);
                    callback.onSuccess(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    callback.onError("Gagal menyimpan data user: " + e.getMessage());
                });
    }

    public void firebaseAuthWithGoogle(String idToken, GoogleAuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            checkUserInFirestore(firebaseUser, callback);
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void sendOtp(String phoneNumber, Activity activity,OtpCallback otpCallback) {
        String formattedPhone = formatPhoneNumber(phoneNumber);

        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        otpCallback.onVerificationCompleted(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        otpCallback.onError(e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        otpCallback.onCodeSent(s, forceResendingToken);
                    }
                };
        // Request ke Firebase
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void verifyOtp(String verificationId, String otpCode, AuthCallback callback) {
        // Membuat kredensial dari ID dan Kode OTP
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otpCode);

        // Mencoba login (sign-in) dengan kredensial tersebut
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        // Verifikasi sukses!
                        callback.onSuccess(user);
                    } else {
                        // Kode salah atau expired
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void sendEmailVerification(AuthCallback callback) {
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    callback.onSuccess(firebaseUser);

                } else {
                    callback.onError(task.getException() !=null ?
                            task.getException().getMessage() : "Gagal mengirim email verifikasi");
                }
            });
        } else {
            callback.onError("User belum login");
        }
    }

    public void reloadUser(AuthCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(user);
                } else {
                    callback.onError(task.getException().getMessage());
                }
            });
        }
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser, GoogleAuthCallback callback) {
        String uid = firebaseUser.getUid();

        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        saveDeviceToken(uid);
                        callback.onSuccess(firebaseUser);
                    } else {
                        callback.onNewUser(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError("Gagal cek database: " + e.getMessage());
                });
    }

    // Fungsi untuk menyimpan User Baru dari Google
//    private void saveNewGoogleUser(FirebaseUser firebaseUser, AuthCallback callback) {
//        String uid = firebaseUser.getUid();
//        String email = firebaseUser.getEmail();
//        String name = firebaseUser.getDisplayName();
//        String phone = firebaseUser.getPhoneNumber(); // Bisa null kalau dari Google
//
//        // Sesuaikan dengan Constructor Model User kamu
//        // User(username, email, password, phoneNumber, isLogin)
//        // Password kita kosongkan/null karena login pakai Google
//        User newUser = new User(name, email, null, phone != null ? phone : "-");
//        newUser.setUserId(uid);
//
//        firestore.collection("users").document(uid)
//                .set(newUser)
//                .addOnSuccessListener(aVoid -> {
//                    // Simpan sukses, baru kabari ViewModel
//                    callback.onSuccess(firebaseUser);
//                })
//                .addOnFailureListener(e -> {
//                    // Jika gagal simpan, kita anggap login gagal juga (agar data konsisten)
//                    firebaseAuth.signOut();
//                    callback.onError("Gagal membuat data user baru: " + e.getMessage());
//                });
//    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void getUserData(String uid, UserDataCallback callback) {
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onDataLoaded(user);
                    } else {
                        callback.onError("Data user tidak ditemukan di database.");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public void linkEmailAndPassword(String email, String password, AuthCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);

            currentUser.linkWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful())    {
                            callback.onSuccess(task.getResult().getUser());
                        } else {
                            callback.onError(task.getException() != null ?
                                    task.getException().getMessage() : "Gagal menghubungkan email");
                        }
                    });
        } else {
            callback.onError("User tidak ditemukan (Sesi habis)");
        }
    }

    public void saveDeviceToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;

                    String token = task.getResult();
                    firestore.collection("users").document(userId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> {});
                });
    }

    public void updateUserProfileImage(Uri imageUri, UpdateProfileCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        com.cloudinary.android.MediaManager.get().upload(imageUri).callback(new com.cloudinary.android.callback.UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, java.util.Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");

                FirebaseFirestore.getInstance().collection("users").document(userId)
                        .update("profileImageUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(imageUrl))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                callback.onError("Gagal upload gambar: " + error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
        }).dispatch();
    }

    public void updateUserData(String newName, String newPhone, UpdateDataCallback callback) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("User belum login");
            return;
        }

        String userId = user.getUid();

        java.util.Map<String, Object> updates = new HashMap<>();
        updates.put("username", newName);
        updates.put("phoneNumber", newPhone);

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout(AuthCallback callback) {
        firebaseAuth.signOut();
        callback.onSuccess(null);
    }
}
