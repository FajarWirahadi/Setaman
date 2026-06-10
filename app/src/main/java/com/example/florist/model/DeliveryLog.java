package com.example.florist.model;

import com.google.firebase.Timestamp;

public class DeliveryLog {
    private String logId;
    private String statusTitle;
    private String description;
    private Timestamp createdAt;

    public DeliveryLog() {}

    public DeliveryLog(String logId, String statusTitle, String description, Timestamp createdAt) {
        this.logId = logId;
        this.statusTitle = statusTitle;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getStatusTitle() { return statusTitle; }
    public void setStatusTitle(String statusTitle) { this.statusTitle = statusTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}