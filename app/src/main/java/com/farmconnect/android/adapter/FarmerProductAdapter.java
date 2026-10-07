package com.farmconnect.android.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
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
import com.farmconnect.android.util.CurrencyFormatter;

import java.util.List;
import java.util.Locale;

public class FarmerProductAdapter
        extends RecyclerView.Adapter<FarmerProductAdapter.ViewHolder> {

    private static final String TAG = "PRODUCT_IMAGE";

    public interface Listener {
        void onEdit(Product product);

        void onDelete(Product product);

        void onToggleAvailability(Product product);
    }

    private final Context context;
    private final List<Product> products;
    private final Listener listener;

    public FarmerProductAdapter(
            Context context,
            List<Product> products,
            Listener listener
    ) {
        this.context = context;
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_farmer_product,
                        parent,
                        false
                );

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
                p.productName != null
                        ? p.productName
                        : ""
        );

        /*
         * Format price using the shared Indian currency formatter so this
         * matches every other price/revenue screen in the app.
         */
        String formattedPrice = CurrencyFormatter.format(p.price);

        holder.details.setText(
                String.format(
                        Locale.getDefault(),
                        "%s / %s  |  Stock: %d",
                        formattedPrice,
                        p.unit != null ? p.unit : "",
                        p.quantity
                )
        );

        // ---------------------------------------------------------
        // Availability switch
        // ---------------------------------------------------------

        holder.availabilitySwitch
                .setOnCheckedChangeListener(null);

        holder.availabilitySwitch
                .setChecked(p.availability);

        holder.availabilitySwitch
                .setOnCheckedChangeListener(
                        (btn, checked) ->
                                listener.onToggleAvailability(p)
                );

        // ---------------------------------------------------------
        // IMAGE
        // ---------------------------------------------------------

        String originalImageUrl = p.imageUrl;

        Log.d(TAG, "----------------------------------------");
        Log.d(TAG, "Product: " + p.productName);
        Log.d(TAG, "Product ID: " + p.productId);
        Log.d(TAG, "Original imageUrl: " + originalImageUrl);

        /*
         * Resolve against the CURRENT backend host, regardless of which
         * host happened to be baked into this URL when it was uploaded.
         * See ImageUrlHelper for why a hardcoded replace-list here is what
         * caused images to keep breaking after every network change.
         */
        String imageUrl = com.farmconnect.android.network.ApiClient.getImageUrl(originalImageUrl);

        Log.d(TAG, "Final image URL: " + imageUrl);

        // Clear any image previously used by this RecyclerView row.
        Glide.with(context)
                .clear(holder.image);

        // ---------------------------------------------------------
        // Load image
        // ---------------------------------------------------------

        if (imageUrl == null
                || imageUrl.trim().isEmpty()) {

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
        // Edit product
        // ---------------------------------------------------------

        holder.itemView.setOnClickListener(
                v -> listener.onEdit(p)
        );

        // ---------------------------------------------------------
        // Delete product
        // ---------------------------------------------------------

        holder.deleteButton.setOnClickListener(v -> {

            Log.d(
                    TAG,
                    "DELETE BUTTON CLICKED: "
                            + p.productName
            );

            listener.onDelete(p);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView image;
        ImageView deleteButton;

        TextView name;
        TextView details;

        Switch availabilitySwitch;

        ViewHolder(View itemView) {
            super(itemView);

            image = itemView.findViewById(
                    R.id.fpImage
            );

            name = itemView.findViewById(
                    R.id.fpName
            );

            details = itemView.findViewById(
                    R.id.fpDetails
            );

            availabilitySwitch = itemView.findViewById(
                    R.id.fpAvailabilitySwitch
            );

            deleteButton = itemView.findViewById(
                    R.id.fpDeleteButton
            );
        }
    }
}

