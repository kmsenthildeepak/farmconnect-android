package com.farmconnect.android.network;

import com.farmconnect.android.model.AuthModels;
import com.farmconnect.android.model.CartModels;
import com.farmconnect.android.model.Dashboard;
import com.farmconnect.android.model.Farmer;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.model.ProductRequest;
import com.farmconnect.android.model.Review;
import com.farmconnect.android.model.ReviewRequest;
import com.farmconnect.android.model.NotificationModels;
import com.farmconnect.android.model.SalesReport;
import com.farmconnect.android.model.UserProfile;
import com.farmconnect.android.model.UpdateAddressRequest;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // =========================================================
    // AUTH
    // =========================================================

    @POST("api/auth/register")
    Call<AuthModels.AuthResponse> register(
            @Body AuthModels.RegisterRequest req
    );

    @POST("api/auth/login")
    Call<AuthModels.AuthResponse> login(
            @Body AuthModels.LoginRequest req
    );

    @POST("api/auth/forgot-password")
    Call<AuthModels.MessageResponse> forgotPassword(
            @Body AuthModels.ForgotPasswordRequest req
    );

    // =========================================================
    // PRODUCTS - PUBLIC
    // =========================================================

    @GET("api/products")
    Call<List<Product>> getAllProducts();

    @GET("api/products/{id}")
    Call<Product> getProduct(
            @Path("id") long id
    );

    @GET("api/products/category/{category}")
    Call<List<Product>> getByCategory(
            @Path("category") String category
    );

    @GET("api/products/organic")
    Call<List<Product>> getOrganic();

    @GET("api/products/search")
    Call<List<Product>> search(
            @Query("q") String query
    );

    @GET("api/products/recent")
    Call<List<Product>> getRecent();

    // =========================================================
    // PRODUCTS - FARMER
    // =========================================================

    @GET("api/products/mine")
    Call<List<Product>> myProducts();

    @POST("api/products")
    Call<Product> createProduct(
            @Body ProductRequest req
    );

    @PUT("api/products/{id}")
    Call<Product> updateProduct(
            @Path("id") long id,
            @Body ProductRequest req
    );

    @DELETE("api/products/{id}")
    Call<Void> deleteProduct(
            @Path("id") long id
    );

    @PATCH("api/products/{id}/availability")
    Call<Product> toggleAvailability(
            @Path("id") long id
    );

    @Multipart
    @POST("api/products/{id}/image")
    Call<Product> uploadProductImage(
            @Path("id") long id,
            @Part MultipartBody.Part file
    );

    // =========================================================
    // FARMER
    // =========================================================

    @GET("api/farmers/{id}")
    Call<Farmer> getFarmer(
            @Path("id") long id
    );

    @GET("api/farmer/profile")
    Call<Farmer> myFarmerProfile();

    @PUT("api/farmer/profile")
    Call<Farmer> updateFarmerProfile(
            @Body com.farmconnect.android.model.UpdateFarmerProfileRequest req
    );

    @GET("api/farmer/sales-report")
    Call<SalesReport> mySalesReport(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("api/farmer/price-suggestion")
    Call<Map<String, Object>> priceSuggestion(
            @Query("category") String category,
            @Query("unit") String unit,
            @Query("organic") boolean organic
    );

    // =========================================================
    // CART
    // =========================================================

    @GET("api/customer/cart")
    Call<CartModels.CartResponse> getCart();

    @POST("api/customer/cart/items")
    Call<CartModels.CartResponse> addToCart(
            @Body CartModels.CartItemRequest req
    );

    @PUT("api/customer/cart/items/{cartItemId}")
    Call<CartModels.CartResponse> updateCartItem(
            @Path("cartItemId") long cartItemId,
            @Query("quantity") int quantity
    );

    @DELETE("api/customer/cart/items/{cartItemId}")
    Call<CartModels.CartResponse> removeCartItem(
            @Path("cartItemId") long cartItemId
    );

    /*
     * Backend:
     *
     * @DeleteMapping
     * public ResponseEntity<Void> clear()
     *
     * therefore Android must expect Void.
     */
    @DELETE("api/customer/cart")
    Call<Void> clearCart();

    // =========================================================
    // ORDERS
    // =========================================================

    @POST("api/customer/orders")
    Call<OrderModels.OrderResponse> placeOrder(
            @Body OrderModels.PlaceOrderRequest req
    );

    @GET("api/customer/orders")
    Call<List<OrderModels.OrderResponse>> myOrders();

    @GET("api/customer/orders/{id}")
    Call<OrderModels.OrderResponse> getOrder(
            @Path("id") long id
    );

    @PATCH("api/customer/orders/{id}/cancel")
    Call<OrderModels.OrderResponse> cancelOrder(
            @Path("id") long id
    );

    @GET("api/farmer/orders")
    Call<List<OrderModels.OrderResponse>> farmerOrders();

    @PATCH("api/farmer/orders/{id}/status")
    Call<OrderModels.OrderResponse> updateOrderStatus(
            @Path("id") long id,
            @Body OrderModels.OrderStatusUpdateRequest req
    );

    // =========================================================
    // REVIEWS
    // =========================================================

    @GET("api/reviews/product/{productId}")
    Call<List<Review>> getProductReviews(
            @Path("productId") long productId
    );

    @GET("api/reviews/my")
    Call<List<Review>> getMyReviews();

    @POST("api/reviews")
    Call<Review> addReview(
            @Body ReviewRequest req
    );

    @PUT("api/reviews/{reviewId}")
    Call<Review> updateReview(
            @Path("reviewId") long reviewId,
            @Body ReviewRequest req
    );

    @DELETE("api/reviews/{reviewId}")
    Call<Void> deleteReview(
            @Path("reviewId") long reviewId
    );

    // =========================================================
    // USER PROFILE / ADDRESS
    // =========================================================

    @GET("api/users/me")
    Call<UserProfile> getMyProfile();

    @retrofit2.http.PUT("api/users/me/address")
    Call<UserProfile> updateMyAddress(
            @Body UpdateAddressRequest req
    );

    @retrofit2.http.PUT("api/users/me/contact")
    Call<UserProfile> updateMyContact(
            @Body com.farmconnect.android.model.UpdateContactRequest req
    );

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    @GET("api/notifications")
    Call<List<NotificationModels.NotificationResponse>> myNotifications();

    @GET("api/notifications/unread-count")
    Call<Map<String, Long>> unreadNotificationCount();

    @PATCH("api/notifications/{id}/read")
    Call<Void> markNotificationRead(
            @Path("id") long id
    );

    @DELETE("api/notifications/{id}")
    Call<Void> deleteNotification(
            @Path("id") long id
    );

    // =========================================================
    // ADMIN
    // =========================================================

    @GET("api/admin/dashboard")
    Call<Dashboard> getDashboard();

    @GET("api/admin/users")
    Call<List<UserProfile>> allUsers();

    @GET("api/admin/customers")
    Call<List<UserProfile>> allCustomers();

    @GET("api/admin/farmers")
    Call<List<Farmer>> allFarmers();

    @GET("api/admin/farmers/search")
    Call<Farmer> searchFarmer(
            @Query("farmerId") Long farmerId,
            @Query("email") String email
    );

    @GET("api/admin/farmers/{farmerId}/products")
    Call<List<Product>> farmerProducts(
            @Path("farmerId") long farmerId
    );

    @GET("api/admin/products")
    Call<List<Product>> adminAllProducts();

    @GET("api/admin/orders")
    Call<List<OrderModels.OrderResponse>> adminAllOrders();

    @DELETE("api/admin/farmers/{farmerId}")
    Call<Void> blockFarmer(@Path("farmerId") long farmerId);

    @PUT("api/admin/farmers/{farmerId}/unblock")
    Call<Void> unblockFarmer(@Path("farmerId") long farmerId);

    @DELETE("api/admin/customers/{userId}")
    Call<Void> blockCustomer(@Path("userId") long userId);

    @PUT("api/admin/customers/{userId}/unblock")
    Call<Void> unblockCustomer(@Path("userId") long userId);

    @GET("api/admin/farmers/pending")
    Call<List<Farmer>> pendingFarmers();

    @PATCH("api/admin/farmers/{id}/verify")
    Call<Farmer> verifyFarmer(
            @Path("id") long id,
            @Body Map<String, String> status
    );
}