package com.farmconnect.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.model.Farmer;

import java.util.List;

public class AdminFarmerAdapter extends RecyclerView.Adapter<AdminFarmerAdapter.ViewHolder> {

    public interface OnActionListener {
        void onBlock(Farmer farmer);
        void onUnblock(Farmer farmer);
    }

    private final Context context;
    private final List<Farmer> farmers;
    private final OnActionListener listener;

    public AdminFarmerAdapter(Context context, List<Farmer> farmers, OnActionListener listener) {
        this.context = context;
        this.farmers = farmers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_farmer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Farmer f = farmers.get(position);
        holder.name.setText(f.farmerName != null ? f.farmerName : "Farmer");
        holder.id.setText("Farmer ID: " + f.farmerId);
        holder.email.setText(f.email != null ? f.email : "-");
        holder.phone.setText(f.phone != null ? f.phone : "-");

        String status = f.isActive
                ? (f.verificationStatus != null ? f.verificationStatus : "VERIFIED")
                : "BLOCKED";
        holder.status.setText(status);
        holder.status.setBackgroundColor(context.getResources().getColor(
                !f.isActive ? R.color.error_red
                        : "PENDING".equals(f.verificationStatus) ? R.color.farm_amber
                        : "REJECTED".equals(f.verificationStatus) ? R.color.error_red
                        : R.color.farm_green));

        holder.blockButton.setEnabled(true);
        if (f.isActive) {
            holder.blockButton.setText("Block");
            holder.blockButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.error_red));
            holder.blockButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBlock(f);
                }
            });
        } else {
            holder.blockButton.setText("Unblock");
            holder.blockButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.farm_green));
            holder.blockButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnblock(f);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return farmers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, id, email, phone, status;
        Button blockButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.afName);
            id = itemView.findViewById(R.id.afId);
            email = itemView.findViewById(R.id.afEmail);
            phone = itemView.findViewById(R.id.afPhone);
            status = itemView.findViewById(R.id.afStatus);
            blockButton = itemView.findViewById(R.id.afBlockButton);
        }
    }
}
