package com.farmconnect.android.ui.customer;

import android.os.Bundle;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.farmconnect.android.R;
import com.farmconnect.android.model.CartModels;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.model.Review;
import com.farmconnect.android.model.ReviewRequest;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView image;
    private TextView name, price, farm, description, rating, stock, reviewList;
    private Button addToCartButton, writeReviewButton;
    private ApiService apiService;
    private long productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        image = findViewById(R.id.detailImage);
        name = findViewById(R.id.detailName);
        price = findViewById(R.id.detailPrice);
        farm = findViewById(R.id.detailFarm);
        description = findViewById(R.id.detailDescription);
        rating = findViewById(R.id.detailRating);
        stock = findViewById(R.id.detailStock);
        reviewList = findViewById(R.id.reviewList);
        writeReviewButton = findViewById(R.id.writeReviewButton);
        addToCartButton = findViewById(R.id.addToCartButton);

        apiService = ApiClient.getApiService(this);
        productId = getIntent().getLongExtra("productId", -1);

        loadProduct();
        addToCartButton.setOnClickListener(v -> addToCart());
        writeReviewButton.setOnClickListener(v -> showReviewDialog());
        loadReviews();
    }

    private void loadProduct() {
        apiService.getProduct(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentProduct = response.body();
                    bind(currentProduct);
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bind(Product p) {
        setTitle(p.productName);
        name.setText(p.productName);
        price.setText(com.farmconnect.android.util.CurrencyFormatter.format(p.price) + " / " + p.unit);
        farm.setText("Sold by " + p.farmName + (p.farmerCity != null ? " - " + p.farmerCity : ""));
        description.setText(
                p.description != null && !p.description.isEmpty()
                        ? p.description
                        : "No description provided."
        );
        rating.setText(
                p.reviewCount > 0
                        ? String.format("★ %.1f (%d reviews)", p.averageRating, p.reviewCount)
                        : "No reviews yet"
        );
        stock.setText(
                p.quantity > 0
                        ? p.quantity + " " + p.unit + " available"
                        : "Out of stock"
        );
        boolean canOrder = p.availability && p.quantity > 0;
        addToCartButton.setEnabled(canOrder);
        addToCartButton.setText(canOrder ? "Add to Cart" : "Out of Stock");

        // Fix old backend image URLs
        String imageUrl = p.imageUrl;

        imageUrl = com.farmconnect.android.network.ApiClient.getImageUrl(imageUrl);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder_produce)
                .error(R.drawable.ic_placeholder_produce)
                .into(image);
    }

    private void addToCart() {
        CartModels.CartItemRequest req = new CartModels.CartItemRequest(productId, 1);
        apiService.addToCart(req).enqueue(new Callback<CartModels.CartResponse>() {
            @Override
            public void onResponse(Call<CartModels.CartResponse> call, Response<CartModels.CartResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProductDetailActivity.this, "Added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProductDetailActivity.this, extractBackendMessage(response, "Could not add to cart"), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CartModels.CartResponse> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Parses the backend's {"message": "..."} error body (see
     * GlobalExceptionHandler#build) so the user sees the actual reason -
     * e.g. "This product is currently out of stock" or the actual review
     * eligibility failure - instead of a generic/misleading hardcoded one.
     */
    private String extractBackendMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                org.json.JSONObject json = new org.json.JSONObject(raw);
                if (json.has("message") && !json.isNull("message")) {
                    return json.getString("message");
                }
            }
        } catch (Exception ignored) {
            // fall through to the fallback message below
        }
        return fallback;
    }

    private void loadReviews() {
        apiService.getProductReviews(productId).enqueue(new Callback<List<Review>>() {
            @Override public void onResponse(Call<List<Review>> call, Response<List<Review>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    reviewList.setText("No reviews yet.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (Review r : response.body()) {
                    sb.append("★ ").append(r.rating).append("  ").append(r.customerName == null ? "Customer" : r.customerName).append("\n")
                      .append(r.comment == null ? "" : r.comment).append("\n\n");
                }
                reviewList.setText(sb.toString().trim());
            }
            @Override public void onFailure(Call<List<Review>> call, Throwable t) {
                reviewList.setText("Could not load reviews.");
            }
        });
    }

    private void showReviewDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 8, 32, 0);
        EditText ratingInput = new EditText(this);
        ratingInput.setHint("Rating (1-5)");
        ratingInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        EditText commentInput = new EditText(this);
        commentInput.setHint("Your review");
        commentInput.setMinLines(3);
        commentInput.setGravity(android.view.Gravity.TOP);
        box.addView(ratingInput);
        box.addView(commentInput);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Write a Review")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit", (d, which) -> submitReview(ratingInput, commentInput))
                .show();
    }

    private void submitReview(EditText ratingInput, EditText commentInput) {
        int stars;
        try { stars = Integer.parseInt(ratingInput.getText().toString().trim()); }
        catch (Exception e) { Toast.makeText(this, "Enter a rating from 1 to 5", Toast.LENGTH_SHORT).show(); return; }
        if (stars < 1 || stars > 5) { Toast.makeText(this, "Rating must be 1 to 5", Toast.LENGTH_SHORT).show(); return; }
        ReviewRequest req = new ReviewRequest();
        req.productId = productId;
        req.rating = stars;
        req.comment = commentInput.getText().toString().trim();
        apiService.addReview(req).enqueue(new Callback<Review>() {
            @Override public void onResponse(Call<Review> call, Response<Review> response) {
                if (response.isSuccessful()) { Toast.makeText(ProductDetailActivity.this, "Review submitted", Toast.LENGTH_SHORT).show(); loadReviews(); }
                else Toast.makeText(ProductDetailActivity.this, extractBackendMessage(response, "Could not submit review"), Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(Call<Review> call, Throwable t) { Toast.makeText(ProductDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }

}
