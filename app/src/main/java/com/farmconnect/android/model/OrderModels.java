package com.farmconnect.android.model;

import java.util.List;

public class OrderModels {

    public static class PlaceOrderRequest {
        public String shippingAddress;
        public String contactPhone;
        public String paymentMethod; // COD, UPI, CARD, NET_BANKING
    }

    public static class OrderStatusUpdateRequest {
        public String status;
        public OrderStatusUpdateRequest(String status) { this.status = status; }
    }

    public static class OrderResponse {
        public long orderId;
        public long customerId;
        public String customerName;
        public String customerPhone;
        public double totalAmount;
        public String shippingAddress;
        public String paymentMethod;
        public String paymentStatus;
        public String orderStatus;
        public List<OrderItem> items;
        public String createdAt;
    }

    public static class OrderItem {
        public long orderItemId;
        public long productId;
        public String productName;
        public String imageUrl;
        public long farmerId;
        public String farmName;
        public int quantity;
        public double price;
        public double subtotal;
    }
}
