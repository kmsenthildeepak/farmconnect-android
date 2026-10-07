
package com.farmconnect.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farmconnect.android.R;
import com.farmconnect.android.model.CartModels;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface Listener {

        void onIncrease(CartModels.CartItem item);

        void onDecrease(CartModels.CartItem item);

        void onRemove(CartModels.CartItem item);
    }

    private final Context context;
    private final List<CartModels.CartItem> items;
    private final Listener listener;

    public CartAdapter(
            Context context,
            List<CartModels.CartItem> items,
            Listener listener
    ) {
        this.context = context;
        this.items = items;
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
                        R.layout.item_cart,
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

        CartModels.CartItem item = items.get(position);

        // ---------------------------------------------------------
        // Product name
        // ---------------------------------------------------------

        String productName =
                item.productName == null
                        || item.productName.trim().isEmpty()
                        ? "Product"
                        : item.productName.trim();

        holder.name.setText(productName);

        // ---------------------------------------------------------
        // Price
        //
        // Use Unicode escape for ₹ so there is no encoding problem.
        // ---------------------------------------------------------

        String priceText =
                com.farmconnect.android.util.CurrencyFormatter.format(item.price)
                        + " x " + item.quantity + " = "
                        + com.farmconnect.android.util.CurrencyFormatter.format(item.subtotal);

        holder.price.setText(priceText);

        // ---------------------------------------------------------
        // Quantity
        // ---------------------------------------------------------

        holder.qty.setText(
                String.valueOf(item.quantity)
        );

        // ---------------------------------------------------------
        // Product image
        //
        // IMPORTANT:
        // There is no ic_placeholder_product drawable in this
        // project.
        //
        // item_cart.xml already gives cartItemImage a background,
        // so do not reference a nonexistent drawable.
        // ---------------------------------------------------------

        String imageUrl = item.imageUrl;

        if (imageUrl != null
                && !imageUrl.trim().isEmpty()) {

             imageUrl = com.farmconnect.android.network.ApiClient.getImageUrl(item.imageUrl);

            Glide.with(context)
                    .clear(holder.image);

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_produce)
                    .error(R.drawable.ic_placeholder_produce)
                    .into(holder.image);

        } else {

            // No product image.
            // Clear any recycled Glide image.
            Glide.with(context)
                    .clear(holder.image);

            holder.image.setImageDrawable(null);
        }

        // ---------------------------------------------------------
        // Decrease button
        // ---------------------------------------------------------

        holder.decrease.setEnabled(
                item.quantity > 1
        );

        holder.decrease.setAlpha(
                item.quantity > 1
                        ? 1.0f
                        : 0.4f
        );

        // ---------------------------------------------------------
        // Increase
        // ---------------------------------------------------------

        holder.increase.setOnClickListener(
                v -> {

                    if (listener != null) {
                        listener.onIncrease(item);
                    }
                }
        );

        // ---------------------------------------------------------
        // Decrease
        // ---------------------------------------------------------

        holder.decrease.setOnClickListener(
                v -> {

                    if (item.quantity > 1
                            && listener != null) {

                        listener.onDecrease(item);
                    }
                }
        );

        // ---------------------------------------------------------
        // Remove
        // ---------------------------------------------------------

        holder.remove.setOnClickListener(
                v -> {

                    if (listener != null) {
                        listener.onRemove(item);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {

        return items == null
                ? 0
                : items.size();
    }

    // =============================================================
    // ViewHolder
    // =============================================================

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView image;

        ImageView increase;

        ImageView decrease;

        ImageView remove;

        TextView name;

        TextView price;

        TextView qty;

        ViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            image =
                    itemView.findViewById(
                            R.id.cartItemImage
                    );

            name =
                    itemView.findViewById(
                            R.id.cartItemName
                    );

            price =
                    itemView.findViewById(
                            R.id.cartItemPrice
                    );

            qty =
                    itemView.findViewById(
                            R.id.cartItemQty
                    );

            increase =
                    itemView.findViewById(
                            R.id.increaseButton
                    );

            decrease =
                    itemView.findViewById(
                            R.id.decreaseButton
                    );

            remove =
                    itemView.findViewById(
                            R.id.removeButton
                    );
        }
    }
}
