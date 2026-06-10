package com.example.florist.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class Notification implements Serializable {
    private String notificationId;
    private String userId;
    private String title;
    private String body;
    private String type;
    private String relatedId;
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {}

    public Notification(String notificationId, String userId, String title, String body, String type, String relatedId, boolean isRead, Timestamp createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.relatedId = relatedId;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}