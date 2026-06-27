package com.example.florist.repository;

import androidx.annotation.Nullable;

import com.example.florist.model.ChatMessage;
import com.example.florist.model.ChatRoom;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference chatRoomsRef = db.collection("chat_rooms");

    public interface RoomCallback {
        void onSuccess(ChatRoom room);
        void onError(String message);
    }

    public interface MessagesCallback {
        void onMessagesUpdated(List<ChatMessage> messages);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }
    public interface ShopNameCallback {
        void onResult(String shopName);
    }

    public interface UnreadCountCallback {
        void onCountUpdated(int totalUnread);
        void onError(String message);
    }

    public void checkShopName(String targetUserId, ShopNameCallback callback) {
        db.collection("shops").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("shopName") != null) {
                        callback.onResult(doc.getString("shopName"));
                    } else {
                        callback.onResult(null); // Bukan toko
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }
    public void createOrGetChatRoom(String buyerId, String sellerId, String buyerName, String sellerName, String sellerImageUrl, RoomCallback callback) {

        String roomIdOption1 = buyerId + "_" + sellerId;
        String roomIdOption2 = sellerId + "_" + buyerId;

        DocumentReference roomDocRef1 = chatRoomsRef.document(roomIdOption1);
        DocumentReference roomDocRef2 = chatRoomsRef.document(roomIdOption2);

        roomDocRef1.get().addOnSuccessListener(documentSnapshot1 -> {
            if (documentSnapshot1.exists()) {
                ChatRoom room = documentSnapshot1.toObject(ChatRoom.class);
                callback.onSuccess(room);
            } else {
                roomDocRef2.get().addOnSuccessListener(documentSnapshot2 -> {
                    if (documentSnapshot2.exists()) {
                        ChatRoom room = documentSnapshot2.toObject(ChatRoom.class);
                        callback.onSuccess(room);
                    } else {
                        ChatRoom newRoom = new ChatRoom();
                        newRoom.setRoomId(roomIdOption1);
                        newRoom.setParticipantIds(Arrays.asList(buyerId, sellerId));
                        newRoom.setBuyerId(buyerId);
                        newRoom.setSellerId(sellerId);
                        newRoom.setBuyerName(buyerName);
                        newRoom.setSellerName(sellerName);
                        newRoom.setSellerImageUrl(sellerImageUrl);
                        newRoom.setLastMessage("");
                        newRoom.setLastMessageTime(Timestamp.now());
                        newRoom.setUnreadBuyer(0);
                        newRoom.setUnreadSeller(0);

                        roomDocRef1.set(newRoom).addOnSuccessListener(aVoid -> {
                            callback.onSuccess(newRoom);
                        }).addOnFailureListener(e -> {
                            callback.onError("Gagal membuat ruang obrolan: " + e.getMessage());
                        });
                    }
                }).addOnFailureListener(e -> callback.onError(e.getMessage()));
            }
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendMessage(String roomId, ChatMessage message, ActionCallback callback) {
        DocumentReference roomRef = chatRoomsRef.document(roomId);
        DocumentReference newMessageRef = roomRef.collection("messages").document();

        message.setMessageId(newMessageRef.getId());

        WriteBatch batch = db.batch();
        batch.set(newMessageRef, message);
        batch.update(roomRef, "lastMessage", message.getText());
        batch.update(roomRef, "lastMessageTime", message.getTimestamp());

        boolean isSenderBuyer = roomId.startsWith(message.getSenderId() + "_");

        String unreadFieldToIncrement = isSenderBuyer ? "unreadSeller" : "unreadBuyer";
        batch.update(roomRef, unreadFieldToIncrement, com.google.firebase.firestore.FieldValue.increment(1));

        batch.commit().addOnSuccessListener(aVoid -> {
            callback.onSuccess();
        }).addOnFailureListener(e -> {
            callback.onError("Gagal mengirim pesan: " + e.getMessage());
        });
    }

    public void listenForMessages(String roomId, MessagesCallback callback) {
        chatRoomsRef.document(roomId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            callback.onError(error.getMessage());
                            return;
                        }

                        List<ChatMessage> messages = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                ChatMessage msg = doc.toObject(ChatMessage.class);
                                messages.add(msg);
                            }
                        }
                        callback.onMessagesUpdated(messages);
                    }
                });
    }

    public void resetUnreadCount(String roomId, String currentUserId) {
        DocumentReference roomRef = chatRoomsRef.document(roomId);
        boolean isCurrentUserBuyer = roomId.startsWith(currentUserId + "_");

        String unreadFieldToReset = isCurrentUserBuyer ? "unreadBuyer" : "unreadSeller";

        roomRef.update(unreadFieldToReset, 0)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                });
    }
    public ListenerRegistration listenForTotalUnread(String currentUserId, UnreadCountCallback callback) {
        return chatRoomsRef.whereArrayContains("participantIds", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    int totalUnread = 0;
                    if (value != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            String buyerId = doc.getString("buyerId");
                            String sellerId = doc.getString("sellerId");

                            // Hitung badge sesuai dengan peran pengguna di room tersebut
                            if (currentUserId.equals(buyerId)) {
                                Long unread = doc.getLong("unreadBuyer");
                                if (unread != null) totalUnread += unread.intValue();
                            } else if (currentUserId.equals(sellerId)) {
                                Long unread = doc.getLong("unreadSeller");
                                if (unread != null) totalUnread += unread.intValue();
                            }
                        }
                    }
                    callback.onCountUpdated(totalUnread);
                });
    }

}