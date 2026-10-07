package com.farmconnect.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farmconnect.android.R;
import com.farmconnect.android.model.Review;
import com.farmconnect.android.network.ApiClient;

import java.util.List;

/**
 * List of the logged-in customer's own reviews, backed by real
 * update/delete endpoints (PUT/DELETE /api/reviews/{id}) - editing here
 * persists to the same backend row Product Details reads from, so there
 * are never two separate copies of a review.
 */
public class MyReviewAdapter extends RecyclerView.Adapter<MyReviewAdapter.ViewHolder> {

    public interface OnEditListener {
        void onEdit(Review review);
    }

    public interface OnDeleteListener {
        void onDelete(Review review);
    }

    private final Context context;
    private final List<Review> reviews;
    private final OnEditListener editListener;
    private final OnDeleteListener deleteListener;

    public MyReviewAdapter(Context context, List<Review> reviews,
                            OnEditListener editListener, OnDeleteListener deleteListener) {
        this.context = context;
        this.reviews = reviews;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_my_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review r = reviews.get(position);

        holder.productName.setText(r.productName != null ? r.productName : "Product");
        holder.rating.setText(starsFor(r.rating) + " " + r.rating + ".0");
        holder.comment.setText(r.comment != null ? r.comment : "");
        holder.date.setText(r.createdAt != null ? r.createdAt.replace("T", " ") : "");

        holder.productImage.setImageDrawable(null);
        String imageUrl = ApiClient.getImageUrl(r.productImage);
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            holder.productImage.setImageResource(R.drawable.ic_placeholder_produce);
        } else {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_produce)
                    .error(R.drawable.ic_placeholder_produce)
                    .centerCrop()
                    .into(holder.productImage);
        }

        holder.editButton.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEdit(r);
            }
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(r);
            }
        });
    }

    private String starsFor(int rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < rating ? "\u2605" : "\u2606");
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, rating, comment, date;
        Button editButton, deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.mrProductImage);
            productName = itemView.findViewById(R.id.mrProductName);
            rating = itemView.findViewById(R.id.mrRating);
            comment = itemView.findViewById(R.id.mrComment);
            date = itemView.findViewById(R.id.mrDate);
            editButton = itemView.findViewById(R.id.mrEditButton);
            deleteButton = itemView.findViewById(R.id.mrDeleteButton);
        }
    }
}
