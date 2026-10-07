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
import com.farmconnect.android.model.UserProfile;

import java.util.List;

public class  AdminCustomerAdapter extends RecyclerView.Adapter<AdminCustomerAdapter.ViewHolder> {

    public interface OnActionListener {
        void onBlock(UserProfile customer);
        void onUnblock(UserProfile customer);
    }

    private final Context context;
    private final List<UserProfile> customers;
    private final OnActionListener listener;

    public AdminCustomerAdapter(Context context, List<UserProfile> customers, OnActionListener listener) {
        this.context = context;
        this.customers = customers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_customer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile c = customers.get(position);
        holder.name.setText(c.name);
        holder.id.setText("Customer ID: " + c.userId);
        holder.email.setText(c.email);
        holder.phone.setText(c.phone != null ? c.phone : "-");

        holder.status.setText(c.isActive ? "Active" : "Blocked");
        holder.status.setBackgroundColor(context.getResources()
                .getColor(c.isActive ? R.color.farm_green : R.color.error_red));

        holder.blockButton.setEnabled(true);
        if (c.isActive) {
            holder.blockButton.setText("Block");
            holder.blockButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.error_red));
            holder.blockButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBlock(c);
                }
            });
        } else {
            holder.blockButton.setText("Unblock");
            holder.blockButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.farm_green));
            holder.blockButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnblock(c);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, id, email, phone, status;
        Button blockButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.acName);
            id = itemView.findViewById(R.id.acId);
            email = itemView.findViewById(R.id.acEmail);
            phone = itemView.findViewById(R.id.acPhone);
            status = itemView.findViewById(R.id.acStatus);
            blockButton = itemView.findViewById(R.id.acBlockButton);
        }
    }
}
