package com.example.florist.model;

import com.google.firebase.Timestamp;

public class Complaint {
    // Data Dasar
    private String complaintId;
    private String rentalId;
    private String orderId;
    private String rentalDuration;
    private String buyerId;
    private String sellerId;

    // Data Tampilan
    private String plantName;
    private String buyerName;
    private String buyerImageUrl;
    private String reason;
    private String description;
    private String evidenceImageUrl;

    // ELEMEN STATE MACHINE & ESKALASI (ENTERPRISE LOGIC)
    private String status;
    private String resolutionType;       // "CHAT_EDUCATION", "PHYSICAL_VISIT", "REPLACEMENT"
    private int rejectionCount;          // Menghitung berapa kali pembeli menolak
    private String rejectionReason;      // Alasan spesifik penolakan terakhir

    // Data Penjual
    private String sellerResponseText;
    private String sellerImageUrl;

    // AUDIT TRAIL TIMESTAMPS (SLA TRACKING)
    private Timestamp createdAt;
    private Timestamp respondedAt;
    private Timestamp visitScheduledAt;
    private Timestamp visitCompletedAt;
    private Timestamp resolvedAt;

    public Complaint() {} // Wajib untuk Firestore

    // --- GETTER & SETTER DATA DASAR ---
    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getRentalId() { return rentalId; }
    public void setRentalId(String rentalId) { this.rentalId = rentalId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getRentalDuration() { return rentalDuration; }
    public void setRentalDuration(String rentalDuration) { this.rentalDuration = rentalDuration; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    // --- GETTER & SETTER DATA TAMPILAN ---
    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerImageUrl() { return buyerImageUrl; }
    public void setBuyerImageUrl(String buyerImageUrl) { this.buyerImageUrl = buyerImageUrl; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEvidenceImageUrl() { return evidenceImageUrl; }
    public void setEvidenceImageUrl(String evidenceImageUrl) { this.evidenceImageUrl = evidenceImageUrl; }

    // --- GETTER & SETTER STATE MACHINE ---
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolutionType() { return resolutionType; }
    public void setResolutionType(String resolutionType) { this.resolutionType = resolutionType; }

    public int getRejectionCount() { return rejectionCount; }
    public void setRejectionCount(int rejectionCount) { this.rejectionCount = rejectionCount; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    // --- GETTER & SETTER DATA PENJUAL ---
    public String getSellerResponseText() { return sellerResponseText; }
    public void setSellerResponseText(String sellerResponseText) { this.sellerResponseText = sellerResponseText; }

    public String getSellerImageUrl() { return sellerImageUrl; }
    public void setSellerImageUrl(String sellerImageUrl) { this.sellerImageUrl = sellerImageUrl; }

    // --- GETTER & SETTER TIMESTAMPS ---
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Timestamp respondedAt) { this.respondedAt = respondedAt; }

    public Timestamp getVisitScheduledAt() { return visitScheduledAt; }
    public void setVisitScheduledAt(Timestamp visitScheduledAt) { this.visitScheduledAt = visitScheduledAt; }

    public Timestamp getVisitCompletedAt() { return visitCompletedAt; }
    public void setVisitCompletedAt(Timestamp visitCompletedAt) { this.visitCompletedAt = visitCompletedAt; }

    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }
}