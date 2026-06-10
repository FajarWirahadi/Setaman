package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Shop;
import com.example.florist.repository.AuthRepository;
import com.example.florist.repository.ProductRepository;
import com.example.florist.repository.ShopRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Calendar;

public class OwnerDashboardViewModel extends ViewModel {
    private ShopRepository shopRepository;
    private AuthRepository authRepository;
    private ProductRepository productRepository;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();


    private MutableLiveData<Integer> countUnpaid = new MutableLiveData<>();
    private MutableLiveData<Integer> countProcessing = new MutableLiveData<>();
    private MutableLiveData<Integer> countShipped = new MutableLiveData<>();
    private MutableLiveData<Integer> countRented = new MutableLiveData<>();
    private MutableLiveData<Integer> countMaintenance = new MutableLiveData<>();
    private MutableLiveData<Integer> countComplaint = new MutableLiveData<>();
    private MutableLiveData<Shop> shopData = new MutableLiveData<>();
    private MutableLiveData<String> updateImageSuccess = new MutableLiveData<>();
    private MutableLiveData<Integer> totalProducts = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> todayMaintenanceCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> overdueMaintenanceCount = new MutableLiveData<>(0);

    public OwnerDashboardViewModel() {
        shopRepository = new ShopRepository();
        productRepository = new ProductRepository();
        authRepository = AuthRepository.getInstance();
    }

    public LiveData<Integer> getCountUnpaid() {return countUnpaid;}
    public LiveData<Integer> getCountProcessing() {return countProcessing;}
    public LiveData<Integer> getCountShipped() {return countShipped;}
    public LiveData<Integer> getCountRented() {return countRented;}
    public LiveData<Integer> getCountMaintenance() {return countMaintenance;}
    public LiveData<Integer> getCountComplaint() {return countComplaint;}
    public LiveData<Shop> getShopData() {return shopData;}
    public LiveData<Integer> getTotalProducts() {return totalProducts;}
    public LiveData<String> getUpdateImageSuccess() {return updateImageSuccess;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}

    public LiveData<Integer> getTodayMaintenanceCount() { return todayMaintenanceCount; }
    public LiveData<Integer> getOverdueMaintenanceCount() { return overdueMaintenanceCount; }

    public void loadDashboardData() {
        isLoading.setValue(true);
        FirebaseUser user = authRepository.getCurrentUser();

        if (user != null ) {
            String shopId = user.getUid();

            shopRepository.getShopById(shopId, new ShopRepository.ShopDataCallback() {
                @Override
                public void onDataLoaded(Shop shop) {
                    isLoading.setValue(false);
                    shopData.setValue(shop);
                }

                @Override
                public void onError(String message) {
                    isLoading.setValue(false);
                    errorMessage.setValue(message);
                }
            });
        } else {
            isLoading.setValue(false);
            errorMessage.setValue("User tidak ditemukan. Silahkan login ulang");
        }

    }

    public void uploadNewProfileImage(Uri imageUri) {
        isLoading.setValue(true);

        shopRepository.updateShopImage(imageUri, new ShopRepository.UpdateImageCallback() {
            @Override
            public void onSuccess(String newImageUrl) {
                isLoading.setValue(false);
                updateImageSuccess.setValue(newImageUrl);
                loadDashboardData();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void loadTotalProducts() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            String ownerId = user.getUid();

            productRepository.getProductCountByOwner(ownerId, new ProductRepository.ProductCountCallback() {
                @Override
                public void onSuccess(int count) {
                    totalProducts.setValue(count);
                }

                @Override
                public void onError(String message) {
                    errorMessage.setValue(message);
                }
            });
        }
    }

    public void loadSellerOrderCounts() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) return;

        String currentSellerId = user.getUid();

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Menunggu Konfirmasi")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countUnpaid.setValue((int) task.getResult().getCount());
                });
        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Diproses")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countProcessing.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Dikirim")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countShipped.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Selesai")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countRented.setValue((int) task.getResult().getCount());
                });

        firestore.collection("rentals")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Dalam Perawatan")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countMaintenance.setValue((int) task.getResult().getCount());
                });

        firestore.collection("rentals")
                .whereEqualTo("sellerId", currentSellerId)
                .whereIn("status", Arrays.asList("Komplain", "PROSES PERBAIKAN"))
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countComplaint.setValue((int) task.getResult().getCount());
                });

    }

    public void fetchMaintenanceAlerts() {
        firestore.collection("rentals")
                .whereEqualTo("status", "Sewa Aktif")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int todayCount = 0;
                    int overdueCount = 0;

                    Calendar todayCal = java.util.Calendar.getInstance();
                    todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    todayCal.set(java.util.Calendar.MINUTE, 0);
                    todayCal.set(java.util.Calendar.SECOND, 0);
                    todayCal.set(java.util.Calendar.MILLISECOND, 0);
                    long todayMidnight = todayCal.getTimeInMillis();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp startStamp = doc.getTimestamp("startDate");
                        Timestamp lastMaintStamp = doc.getTimestamp("lastMaintenanceDate");

                        if (startStamp != null) {
                            Calendar startCal = java.util.Calendar.getInstance();
                            startCal.setTimeInMillis(startStamp.toDate().getTime());
                            startCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                            startCal.set(java.util.Calendar.MINUTE, 0);
                            startCal.set(java.util.Calendar.SECOND, 0);
                            startCal.set(java.util.Calendar.MILLISECOND, 0);

                            long diffMillis = todayMidnight - startCal.getTimeInMillis();
                            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

                            boolean isDueToday = (diffDays > 0 && diffDays % 3 == 0);

                            long lastMaintMillis = 0;
                            if (lastMaintStamp != null) {
                                Calendar lastCal = Calendar.getInstance();
                                lastCal.setTimeInMillis(lastMaintStamp.toDate().getTime());
                                lastCal.set(Calendar.HOUR_OF_DAY, 0);
                                lastCal.set(Calendar.MINUTE, 0);
                                lastCal.set(Calendar.SECOND, 0);
                                lastCal.set(Calendar.MILLISECOND, 0);
                                lastMaintMillis = lastCal.getTimeInMillis();
                            }

                            if (isDueToday) {
                                if (lastMaintMillis < todayMidnight) todayCount++;
                            } else if (diffDays > 0) {
                                long lastTargetDay = diffDays - (diffDays % 3);
                                long lastTargetMillis = startCal.getTimeInMillis() + (lastTargetDay * 24 * 60 * 60 * 1000L);

                                if (lastMaintMillis < lastTargetMillis) overdueCount++;
                            }
                        }
                    }

                    todayMaintenanceCount.setValue(todayCount);
                    overdueMaintenanceCount.setValue(overdueCount);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Gagal memuat jadwal perawatan: " + e);
                });
    }
}
