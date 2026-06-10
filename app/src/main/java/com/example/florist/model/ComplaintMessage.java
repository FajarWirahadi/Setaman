package com.example.florist.model;

import com.google.firebase.Timestamp;

public class ComplaintMessage {
    private String messageId;
    private String senderRole;
    private String senderName;
    private String senderPhotoUrl;
    private String messageText;
    private Timestamp createdAt;

    public ComplaintMessage() {
    }

    public ComplaintMessage(String messageId, String senderRole, String senderName, String senderPhotoUrl, String messageText, Timestamp createdAt) {
        this.messageId = messageId;
        this.senderRole = senderRole;
        this.senderName = senderName;
        this.senderPhotoUrl = senderPhotoUrl;
        this.messageText = messageText;
        this.createdAt = createdAt;
    }

    // --- GETTER & SETTER ---
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhotoUrl() { return senderPhotoUrl; }
    public void setSenderPhotoUrl(String senderPhotoUrl) { this.senderPhotoUrl = senderPhotoUrl; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}