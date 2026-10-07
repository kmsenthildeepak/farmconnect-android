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
import com.farmconnect.android.model.Product;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.util.CurrencyFormatter;

import java.util.List;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> products;

    public AdminProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = products.get(position);

        holder.name.setText(p.productName);
        holder.farm.setText((p.farmName != null ? p.farmName : "Unknown farm")
                + (p.farmerCity != null ? " - " + p.farmerCity : ""));
        holder.category.setText((p.category != null ? p.category : "-")
                + (p.isOrganic ? " - Organic" : ""));
        holder.price.setText(CurrencyFormatter.format(p.price) + " / " + p.unit);
        holder.stockAvailability.setText("Stock: " + p.quantity + " " + p.unit
                + " - " + (p.availability ? "Available" : "Unavailable"));

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
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, farm, category, price, stockAvailability;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.apImage);
            name = itemView.findViewById(R.id.apName);
            farm = itemView.findViewById(R.id.apFarm);
            category = itemView.findViewById(R.id.apCategory);
            price = itemView.findViewById(R.id.apPrice);
            stockAvailability = itemView.findViewById(R.id.apStockAvailability);
        }
    }
}
