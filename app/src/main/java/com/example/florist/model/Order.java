package com.example.florist.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Order implements Serializable {
    private String orderId;
    private String buyerId;
    private List<String> sellerIds;
    private List<CartItem> items; // Pesanan bisa berisi banyak barang sekaligus
    private String deliveryAddress;
    private String paymentMethod;
    private long subTotal;
    private long shippingCost;
    private long grandTotal;
    private String status;
    private Date orderDate;

    // 1. Konstruktor Kosong (SANGAT WAJIB UNTUK FIRESTORE)
    public Order() {
    }

    // 2. Konstruktor Penuh
    public Order(String orderId, String buyerId, List<String> sellerIds, List<CartItem> items,
                 String deliveryAddress, String paymentMethod, long subTotal,
                 long shippingCost, long grandTotal, String status, Date orderDate) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerIds = sellerIds;
        this.items = items;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.subTotal = subTotal;
        this.shippingCost = shippingCost;
        this.grandTotal = grandTotal;
        this.status = status;
        this.orderDate = orderDate;
    }

    // ==========================================
    // 3. GETTER DAN SETTER
    // ==========================================
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public List<String> getSellerIds() { return sellerIds; }
    public void setSellerIds(List<String> sellerIds) { this.sellerIds = sellerIds; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getSubTotal() { return subTotal; }
    public void setSubTotal(long subTotal) { this.subTotal = subTotal; }

    public long getShippingCost() { return shippingCost; }
    public void setShippingCost(long shippingCost) { this.shippingCost = shippingCost; }

    public long getGrandTotal() { return grandTotal; }
    public void setGrandTotal(long grandTotal) { this.grandTotal = grandTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
}