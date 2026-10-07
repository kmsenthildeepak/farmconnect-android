package com.farmconnect.android.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farmconnect.android.R;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.ui.farmer.AddEditProductActivity;
import com.farmconnect.android.util.CurrencyFormatter;

import java.util.List;

/**
 * Grid adapter for the Farmer Home screen. Visually similar to the
 * customer product grid (card, image, name, price) but scoped to the
 * farmer's own products (see ApiService#myProducts, enforced server-side)
 * and showing stock/availability instead of ratings/reviews - a farmer
 * manages inventory here, they don't buy from themselves.
 *
 * Tapping a card opens AddEditProductActivity for quick editing; full
 * manage/delete/availability-toggle actions remain on Manage My Products.
 */
public class FarmerHomeProductAdapter extends RecyclerView.Adapter<FarmerHomeProductAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> products;

    public FarmerHomeProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_farmer_home_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = products.get(position);

        holder.name.setText(p.productName);
        holder.price.setText(CurrencyFormatter.format(p.price) + " / " + p.unit);
        holder.stock.setText(p.quantity + " " + p.unit + " in stock");

        holder.organicBadge.setVisibility(p.isOrganic ? View.VISIBLE : View.GONE);

        holder.availabilityBadge.setText(p.availability ? "Available" : "Unavailable");
        holder.availabilityBadge.setBackgroundColor(context.getResources()
                .getColor(p.availability ? R.color.farm_green : R.color.error_red));

        // Recycled views must be cleared before loading, otherwise a fast
        // scroll can briefly show the previous row's image on this card.
        holder.image.setImageDrawable(null);
        String imageUrl = ApiClient.getImageUrl(p.imageUrl);
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            holder.image.setImageResource(R.drawable.ic_placeholder_produce);
        } else {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_produce)
                    .error(R.drawable.ic_placeholder_produce)
                    .centerCrop()
                    .into(holder.image);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditProductActivity.class);
            intent.putExtra("productId", p.productId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, stock, organicBadge, availabilityBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.fhImage);
            name = itemView.findViewById(R.id.fhName);
            price = itemView.findViewById(R.id.fhPrice);
            stock = itemView.findViewById(R.id.fhStock);
            organicBadge = itemView.findViewById(R.id.fhOrganicBadge);
            availabilityBadge = itemView.findViewById(R.id.fhAvailabilityBadge);
        }
    }
}
