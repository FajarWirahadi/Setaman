package com.example.florist.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.List;

public class ChatRoom implements Serializable {
    private String roomId;
    private List<String> participantIds;
    private String buyerId;
    private String sellerId;
    private String buyerName;
    private String sellerName;
    private String sellerImageUrl;
    private String lastMessage;
    private Timestamp lastMessageTime;
    private int unreadBuyer;
    private int unreadSeller;

    public ChatRoom() {}

    public ChatRoom(String roomId, List<String> participantIds, String buyerId, String sellerId, String buyerName,
                    String sellerName, String sellerImageUrl, String lastMessage, Timestamp lastMessageTime, int unreadBuyer, int unreadSeller) {
        this.roomId = roomId;
        this.participantIds = participantIds;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.sellerImageUrl = sellerImageUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadBuyer = unreadBuyer;
        this.unreadSeller = unreadSeller;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerImageUrl() {
        return sellerImageUrl;
    }

    public void setSellerImageUrl(String sellerImageUrl) {
        this.sellerImageUrl = sellerImageUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadBuyer() {
        return unreadBuyer;
    }

    public void setUnreadBuyer(int unreadBuyer) {
        this.unreadBuyer = unreadBuyer;
    }

    public int getUnreadSeller() {
        return unreadSeller;
    }

    public void setUnreadSeller(int unreadSeller) {
        this.unreadSeller = unreadSeller;
    }

}