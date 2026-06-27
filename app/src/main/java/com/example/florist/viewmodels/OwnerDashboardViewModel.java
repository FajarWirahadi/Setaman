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
    private MutableLiveData<Integer> countUrgentComplaint = new MutableLiveData<>(0);

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
    public LiveData<Integer> getCountUrgentComplaint() {return countUrgentComplaint;}
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
                .whereEqualTo("status", "MENUNGGU KONFIRMASI")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countUnpaid.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "DIPROSES")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countProcessing.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "DIKIRIM")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countShipped.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "SELESAI")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countRented.setValue((int) task.getResult().getCount());
                });

        firestore.collection("complaints")
                .whereEqualTo("sellerId", currentSellerId)
                .whereIn("status", Arrays.asList("MENUNGGU RESPON", "PROSES PERBAIKAN"))
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countComplaint.setValue((int) task.getResult().getCount());
                });
    }
    public void fetchMaintenanceAlerts() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) return;

        String currentSellerId = user.getUid();

        firestore.collection("rentals")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "SEWA AKTIF")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int todayCount = 0;
                    int overdueCount = 0;

                    Calendar todayCal = Calendar.getInstance();
                    todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0);
                    long todayMidnight = todayCal.getTimeInMillis();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp startStamp = doc.getTimestamp("startDate");
                        Timestamp endStamp = doc.getTimestamp("endDate"); // AMBIL END DATE
                        Timestamp lastMaintStamp = doc.getTimestamp("lastMaintenanceDate");

                        if (startStamp != null) {
                            // Hitung Batas Akhir Sewa
                            long endMillis = Long.MAX_VALUE;
                            if (endStamp != null) {
                                Calendar endCal = Calendar.getInstance();
                                endCal.setTimeInMillis(endStamp.toDate().getTime());
                                endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
                                endMillis = endCal.getTimeInMillis();
                            }

                            // ENTERPRISE LOGIC: JIKA MASA SEWA SUDAH HABIS, HENTIKAN SEMUA PERINTAH PERAWATAN!
                            if (todayMidnight > endMillis) {
                                continue; // Lompati pesanan ini. Jangan ditambahkan ke Overdue/Today!
                            }

                            Calendar startCal = Calendar.getInstance();
                            startCal.setTimeInMillis(startStamp.toDate().getTime());
                            startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);

                            long diffMillis = todayMidnight - startCal.getTimeInMillis();
                            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

                            long lastMaintMillis = 0;
                            if (lastMaintStamp != null) {
                                Calendar lastCal = Calendar.getInstance();
                                lastCal.setTimeInMillis(lastMaintStamp.toDate().getTime());
                                lastCal.set(Calendar.HOUR_OF_DAY, 0); lastCal.set(Calendar.MINUTE, 0); lastCal.set(Calendar.SECOND, 0); lastCal.set(Calendar.MILLISECOND, 0);
                                lastMaintMillis = lastCal.getTimeInMillis();
                            }

                            // MATEMATIKA JADWAL PERAWATAN
                            long targetDays = diffDays - (diffDays % 3);

                            if (targetDays > 0) {
                                long targetMillis = startCal.getTimeInMillis() + (targetDays * 24 * 60 * 60 * 1000L);

                                if (lastMaintMillis < targetMillis) {
                                    if (diffDays % 3 == 0) {
                                        todayCount++;
                                    } else {
                                        overdueCount++;
                                    }
                                }
                            }
                        }
                    }

                    todayMaintenanceCount.setValue(todayCount);
                    overdueMaintenanceCount.setValue(overdueCount);
                    countMaintenance.setValue(todayCount + overdueCount);
                })
                .addOnFailureListener(e -> errorMessage.setValue("Gagal memuat jadwal perawatan: " + e.getMessage()));
    }
}
