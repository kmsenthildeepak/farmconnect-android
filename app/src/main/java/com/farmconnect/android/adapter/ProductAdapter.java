package com.farmconnect.android.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.farmconnect.android.R;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.ui.customer.ProductDetailActivity;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private static final String TAG = "PRODUCT_IMAGE";

    private final Context context;
    private final List<Product> products;

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        Product p = products.get(position);

        // ---------------------------------------------------------
        // Product information
        // ---------------------------------------------------------

        holder.name.setText(
                p.productName != null ? p.productName : ""
        );

        holder.price.setText(
                com.farmconnect.android.util.CurrencyFormatter.format(p.price)
                        + " / " + (p.unit != null ? p.unit : "")
        );

        holder.farm.setText(
                p.farmName != null ? p.farmName : ""
        );

        holder.organicBadge.setVisibility(
                p.isOrganic ? View.VISIBLE : View.GONE
        );

        holder.rating.setText(
                p.reviewCount > 0
                        ? String.format("★ %.1f (%d)", p.averageRating, p.reviewCount)
                        : "No reviews yet"
        );

        // ---------------------------------------------------------
        // Image URL
        // ---------------------------------------------------------

        String originalImageUrl = p.imageUrl;
        String imageUrl = originalImageUrl;

        Log.d(TAG, "----------------------------------------");
        Log.d(TAG, "Product: " + p.productName);
        Log.d(TAG, "Product ID: " + p.productId);
        Log.d(TAG, "Original imageUrl: " + originalImageUrl);

        // Resolved against the current backend host (ApiClient.BASE_URL),
        // regardless of which host was baked into this URL originally -
        // see ImageUrlHelper for details.
        imageUrl = com.farmconnect.android.network.ApiClient.getImageUrl(imageUrl);

        Log.d(TAG, "Final image URL: " + imageUrl);

        // ---------------------------------------------------------
        // Clear previous RecyclerView image
        // ---------------------------------------------------------

        Glide.with(context)
                .clear(holder.image);

        // ---------------------------------------------------------
        // Load image using Glide
        // ---------------------------------------------------------

        if (imageUrl == null || imageUrl.trim().isEmpty()) {

            Log.w(
                    TAG,
                    "No image URL for product: "
                            + p.productName
            );

            holder.image.setImageResource(
                    R.drawable.ic_placeholder_produce
            );

        } else {

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(
                            R.drawable.ic_placeholder_produce
                    )
                    .error(
                            R.drawable.ic_placeholder_produce
                    )
                    .listener(
                            new RequestListener<Drawable>() {

                                @Override
                                public boolean onLoadFailed(
                                        GlideException e,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource
                                ) {

                                    Log.e(
                                            TAG,
                                            "IMAGE LOAD FAILED"
                                    );

                                    Log.e(
                                            TAG,
                                            "Product: "
                                                    + p.productName
                                    );

                                    Log.e(
                                            TAG,
                                            "Product ID: "
                                                    + p.productId
                                    );

                                    Log.e(
                                            TAG,
                                            "URL: "
                                                    + model
                                    );

                                    if (e != null) {
                                        Log.e(
                                                TAG,
                                                "Glide exception: "
                                                        + e.getMessage(),
                                                e
                                        );
                                    }

                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(
                                        Drawable resource,
                                        Object model,
                                        Target<Drawable> target,
                                        DataSource dataSource,
                                        boolean isFirstResource
                                ) {

                                    Log.d(
                                            TAG,
                                            "IMAGE LOADED SUCCESSFULLY"
                                    );

                                    Log.d(
                                            TAG,
                                            "Product: "
                                                    + p.productName
                                    );

                                    Log.d(
                                            TAG,
                                            "URL: "
                                                    + model
                                    );

                                    Log.d(
                                            TAG,
                                            "Data source: "
                                                    + dataSource
                                    );

                                    return false;
                                }
                            }
                    )
                    .into(holder.image);
        }

        // ---------------------------------------------------------
        // Product click
        // ---------------------------------------------------------

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    context,
                    ProductDetailActivity.class
            );

            intent.putExtra(
                    "productId",
                    p.productId
            );

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateData(List<Product> newProducts) {

        products.clear();
        products.addAll(newProducts);

        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;

        TextView name;
        TextView price;
        TextView farm;
        TextView rating;
        TextView organicBadge;

        ViewHolder(View itemView) {
            super(itemView);

            image = itemView.findViewById(
                    R.id.productImage
            );

            name = itemView.findViewById(
                    R.id.productName
            );

            price = itemView.findViewById(
                    R.id.productPrice
            );

            farm = itemView.findViewById(
                    R.id.productFarm
            );

            rating = itemView.findViewById(
                    R.id.productRating
            );

            organicBadge = itemView.findViewById(
                    R.id.organicBadge
            );
        }
    }
}





