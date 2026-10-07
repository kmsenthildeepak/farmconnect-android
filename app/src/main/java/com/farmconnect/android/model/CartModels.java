package com.farmconnect.android.model;

import java.util.List;

public class CartModels {

    public static class CartItemRequest {
        public long productId;
        public int quantity;
        public CartItemRequest(long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    public static class CartResponse {
        public long cartId;
        public List<CartItem> items;
        public double total;
    }

    public static class CartItem {
        public long cartItemId;
        public long productId;
        public String productName;
        public String imageUrl;
        public double price;
        public int quantity;
        public double subtotal;
        public int availableStock;
    }
}
