package com.example.florist.repository;

import com.example.florist.model.CartItem;
import com.example.florist.model.DeliveryAddress;
import com.example.florist.model.Order;
import com.example.florist.model.Rental;
import com.example.florist.model.User;
import com.google.firebase.Timestamp; // [ENTERPRISE FIX]: Import Timestamp
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar; // [ENTERPRISE FIX]: Import Calendar
import java.util.Date;     // [ENTERPRISE FIX]: Import Date
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess(String orderId);
        void onError(String error);
    }

    public interface TokenCallback {
        void onTokenReceived(String token);
        void onError(String error);
    }

    public void getUserProfile(String userId, DataCallback<User> callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess(doc.toObject(User.class));
                    } else {
                        callback.onError("User tidak ditemukan");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getMainAddress(String userId, DataCallback<DeliveryAddress> callback) {
        db.collection("users").document(userId).collection("addresses")
                .whereEqualTo("mainAddress", true).limit(1).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        callback.onSuccess(query.getDocuments().get(0).toObject(DeliveryAddress.class));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getCartItems(String userId, DataCallback<List<CartItem>> callback) {
        db.collection("users").document(userId).collection("cart").get()
                .addOnSuccessListener(query -> {
                    List<CartItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        CartItem item = doc.toObject(CartItem.class);
                        if (item != null) items.add(item);
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void processCheckout(List<CartItem> cartItems, String buyerId, String buyerName,
                                DeliveryAddress deliveryAddress, String paymentMethod,
                                double deliveryFee, boolean isDirectBuy, ActionCallback callback) {

        Map<String, List<CartItem>> groupedOrders = new HashMap<>();

        for (CartItem item : cartItems) {
            String sellerId = item.getOwnerId();
            if (!groupedOrders.containsKey(sellerId)) {
                groupedOrders.put(sellerId, new ArrayList<>());
            }
            groupedOrders.get(sellerId).add(item);
        }

        WriteBatch batch = db.batch();
        String firstOrderId = null;

        for (Map.Entry<String, List<CartItem>> entry : groupedOrders.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> itemsForThisSeller = entry.getValue();

            DocumentReference orderRef = db.collection("orders").document();

            if (firstOrderId == null) {
                firstOrderId = orderRef.getId();
            }

            double totalAmount = 0;
            for (CartItem item : itemsForThisSeller) {
                long duration = item.getDurationValue() > 0 ? item.getDurationValue() : 1;
                long itemBasePrice = (long) item.getPrice();

                String type = item.getDurationType() != null ? item.getDurationType() : "";
                int multiplier = 1;
                if (type.equalsIgnoreCase("Mingguan") || type.equalsIgnoreCase("Minggu")) {
                    multiplier = 7;
                } else if (type.equalsIgnoreCase("Bulanan") || type.equalsIgnoreCase("Bulan")) {
                    multiplier = 30;
                }

                totalAmount += (itemBasePrice * item.getQuantity() * duration * multiplier);
            }

            totalAmount += deliveryFee;

            Order newOrder = new Order(orderRef.getId(), buyerId, sellerId, itemsForThisSeller, totalAmount, deliveryAddress, paymentMethod);
            batch.set(orderRef, newOrder);

            for (CartItem item : itemsForThisSeller) {
                DocumentReference rentalRef = db.collection("rentals").document();

                Rental newRental = new Rental();
                newRental.setRentalId(rentalRef.getId());
                newRental.setOrderId(orderRef.getId());
                newRental.setBuyerId(buyerId);
                newRental.setSellerId(sellerId);
                newRental.setSellerName(item.getShopName());
                newRental.setBuyerName(buyerName);
                newRental.setReceiverName(deliveryAddress != null ? deliveryAddress.getReceiverName() : buyerName);
                newRental.setDeliveryAddress(deliveryAddress);
                newRental.setPlantName(item.getName());
                newRental.setPlantImageUrl(item.getImageUrl());

                long durationVal = item.getDurationValue() > 0 ? item.getDurationValue() : 1;
                long rentalBasePrice = (long) item.getPrice();

                String type = item.getDurationType() != null ? item.getDurationType() : "";
                int multiplier = 1;
                if (type.equalsIgnoreCase("Mingguan") || type.equalsIgnoreCase("Minggu")) {
                    multiplier = 7;
                } else if (type.equalsIgnoreCase("Bulanan") || type.equalsIgnoreCase("Bulan")) {
                    multiplier = 30;
                }

                newRental.setTotalAmount(rentalBasePrice * item.getQuantity() * durationVal * multiplier);
                newRental.setStatus("BELUM BAYAR"); // Status awal

                // ==============================================================
                // [ENTERPRISE FIX]: Injeksi Waktu dan Kalkulasi Otomatis
                // ==============================================================
                Calendar calendar = Calendar.getInstance();
                Date startDate = calendar.getTime(); // Hari ini saat checkout

                // Kalkulasi tanggal selesai berdasarkan durasi dan tipe
                if (type.equalsIgnoreCase("Mingguan") || type.equalsIgnoreCase("Minggu")) {
                    calendar.add(Calendar.WEEK_OF_YEAR, (int) durationVal);
                } else if (type.equalsIgnoreCase("Bulanan") || type.equalsIgnoreCase("Bulan")) {
                    calendar.add(Calendar.MONTH, (int) durationVal);
                } else {
                    // Default Harian
                    calendar.add(Calendar.DAY_OF_YEAR, (int) durationVal);
                }
                Date endDate = calendar.getTime();

                // Assign Timestamp ke Model Rental
                newRental.setCreatedAt(new Timestamp(startDate));
                newRental.setStartDate(new Timestamp(startDate));
                newRental.setEndDate(new Timestamp(endDate));
                // ==============================================================

                batch.set(rentalRef, newRental);
            }
        }

        if (!isDirectBuy) {
            for (CartItem item : cartItems) {
                DocumentReference cartRef = db.collection("users").document(buyerId).collection("cart").document(item.getProductId());
                batch.delete(cartRef);
            }
        }
        final String finalOrderId = firstOrderId;

        // COMMIT BATCH HANYA DILAKUKAN 1 KALI DI PALING AKHIR
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(finalOrderId))
                .addOnFailureListener(e -> callback.onError("Gagal checkout: " + e.getMessage()));
    }

    public ListenerRegistration listenForPaymentToken(String orderId, TokenCallback callback) {
        return db.collection("orders").document(orderId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        callback.onError(e.getMessage());
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        String token = doc.getString("snapToken");
                        if (token != null && !token.isEmpty()) {
                            callback.onTokenReceived(token);
                        }
                    }
                });
    }
}