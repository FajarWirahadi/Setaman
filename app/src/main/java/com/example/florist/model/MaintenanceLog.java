package com.example.florist.model;

import com.google.firebase.Timestamp;

public class MaintenanceLog {
    private String logId;
    private String description;
    private String imageUrl;
    private Timestamp createdAt;

    public MaintenanceLog() {
    }

    public MaintenanceLog(String logId, String description, String imageUrl, Timestamp createdAt) {
        this.logId = logId;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
