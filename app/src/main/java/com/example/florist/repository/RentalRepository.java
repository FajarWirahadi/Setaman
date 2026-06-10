package com.example.florist.repository;

import com.example.florist.model.Rental;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RentalRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface RentalListCallback {
        void onSuccess(List<Rental> rentals);
        void onError(String message);
    }

    public ListenerRegistration listenToBuyerRentals(RentalListCallback callback) {

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError("Harap login terlebih dahulu.");
            return null;
        }

        String buyerId = user.getUid();

        return firestore.collection("rentals")
                .whereEqualTo("buyerId", buyerId)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError("Gagal memuat data sewa: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Rental> rentalList = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Rental rental = doc.toObject(Rental.class);
                            if (rental != null) {
                                if (rental.getRentalId() == null) rental.setRentalId(doc.getId());
                                rentalList.add(rental);
                            }
                        }
                        callback.onSuccess(rentalList);
                    }
                });
    }
}