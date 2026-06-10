package com.example.florist.repository;

import androidx.annotation.Nullable;

import com.example.florist.model.ChatRoom;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class InboxRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference chatRoomsRef = db.collection("chat_rooms");

    public interface InboxCallback {
        void onInboxUpdated(List<ChatRoom> chatRooms);
        void onError(String error);
    }

    public void listenToInbox(String userId, InboxCallback callback) {
        chatRoomsRef.whereArrayContains("participantIds", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            callback.onError(error.getMessage());
                            return;
                        }

                        List<ChatRoom> rooms = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                ChatRoom room = doc.toObject(ChatRoom.class);
                                rooms.add(room);
                            }
                        }
                        callback.onInboxUpdated(rooms);
                    }
                });
    }
}