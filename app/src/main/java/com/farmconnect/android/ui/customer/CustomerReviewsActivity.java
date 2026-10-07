package com.farmconnect.android.ui.customer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.MyReviewAdapter;
import com.farmconnect.android.model.Review;
import com.farmconnect.android.model.ReviewRequest;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reached from the sidebar's "My Reviews" item.
 *
 * Real backend-backed list via GET /api/reviews/my, with real edit
 * (PUT /api/reviews/{id}) and delete (DELETE /api/reviews/{id}) - both
 * ownership-checked server-side in ReviewServiceImpl. Editing/deleting here
 * changes the same row Product Details reads, so there's never a separate
 * "local" copy to keep in sync.
 */
public class CustomerReviewsActivity extends AppCompatActivity {

    private RecyclerView reviewsList;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyState;
    private MyReviewAdapter adapter;
    private final List<Review> reviews = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reviews);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        reviewsList = findViewById(R.id.reviewsList);
        swipeRefresh = findViewById(R.id.reviewsSwipeRefresh);
        emptyState = findViewById(R.id.reviewsEmptyState);

        apiService = ApiClient.getApiService(this);

        reviewsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyReviewAdapter(this, reviews, this::showEditDialog, this::confirmDelete);
        reviewsList.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadMyReviews);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyReviews();
    }

    private void loadMyReviews() {
        swipeRefresh.setRefreshing(true);
        apiService.getMyReviews().enqueue(new Callback<List<Review>>() {
            @Override
            public void onResponse(Call<List<Review>> call, Response<List<Review>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CustomerReviewsActivity.this,
                            "Could not load your reviews", Toast.LENGTH_SHORT).show();
                    return;
                }
                reviews.clear();
                reviews.addAll(response.body());
                adapter.notifyDataSetChanged();

                boolean empty = reviews.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                reviewsList.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<Review>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CustomerReviewsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEditDialog(Review review) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 8, 32, 0);

        EditText ratingInput = new EditText(this);
        ratingInput.setHint("Rating (1-5)");
        ratingInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        ratingInput.setText(String.valueOf(review.rating));

        EditText commentInput = new EditText(this);
        commentInput.setHint("Your review");
        commentInput.setMinLines(3);
        commentInput.setGravity(Gravity.TOP);
        commentInput.setText(review.comment);

        box.addView(ratingInput);
        box.addView(commentInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Review")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, which) -> saveEdit(review, ratingInput, commentInput))
                .show();
    }

    private void saveEdit(Review review, EditText ratingInput, EditText commentInput) {
        int stars;
        try {
            stars = Integer.parseInt(ratingInput.getText().toString().trim());
        } catch (Exception e) {
            Toast.makeText(this, "Enter a rating from 1 to 5", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stars < 1 || stars > 5) {
            Toast.makeText(this, "Rating must be 1 to 5", Toast.LENGTH_SHORT).show();
            return;
        }

        ReviewRequest req = new ReviewRequest();
        req.productId = review.productId;
        req.rating = stars;
        req.comment = commentInput.getText().toString().trim();

        apiService.updateReview(review.reviewId, req).enqueue(new Callback<Review>() {
            @Override
            public void onResponse(Call<Review> call, Response<Review> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CustomerReviewsActivity.this, "Review updated", Toast.LENGTH_SHORT).show();
                    loadMyReviews();
                } else {
                    Toast.makeText(CustomerReviewsActivity.this, "Could not update review", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Review> call, Throwable t) {
                Toast.makeText(CustomerReviewsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete(Review review) {
        new AlertDialog.Builder(this)
                .setTitle("Delete review")
                .setMessage("Are you sure you want to delete this review?")
                .setPositiveButton("Delete", (d, which) -> deleteReview(review))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReview(Review review) {
        apiService.deleteReview(review.reviewId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CustomerReviewsActivity.this, "Review deleted", Toast.LENGTH_SHORT).show();
                    loadMyReviews();
                } else {
                    Toast.makeText(CustomerReviewsActivity.this, "Could not delete review", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CustomerReviewsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
