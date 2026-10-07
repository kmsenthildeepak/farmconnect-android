package com.farmconnect.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.util.CurrencyFormatter;

import java.util.Arrays;
import java.util.List;

import android.graphics.Color;
import java.util.Collections;

public class FarmerOrderAdapter extends RecyclerView.Adapter<FarmerOrderAdapter.ViewHolder> {

    public interface Listener {
        void onUpdateStatus(OrderModels.OrderResponse order, String newStatus);
    }

    private final Context context;
    private final List<OrderModels.OrderResponse> orders;
    private final Listener listener;

    public FarmerOrderAdapter(Context context, List<OrderModels.OrderResponse> orders, Listener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
    }

    public static List<String> getAllowedTransitions(String currentStatus) {
        if (currentStatus == null) {
            return Collections.emptyList();
        }
        switch (currentStatus.trim().toUpperCase()) {
            case "PENDING":
                return Arrays.asList("ACCEPTED", "REJECTED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED");
            case "ACCEPTED":
                return Arrays.asList("REJECTED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED");
            case "PACKED":
                return Arrays.asList("REJECTED", "OUT_FOR_DELIVERY", "DELIVERED");
            case "OUT_FOR_DELIVERY":
                return Collections.singletonList("DELIVERED");
            case "REJECTED":
            case "DELIVERED":
            case "CANCELLED":
            default:
                return Collections.emptyList();
        }
    }

    public static boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        List<String> allowed = getAllowedTransitions(currentStatus);
        return allowed.contains(targetStatus.trim().toUpperCase());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_farmer_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderModels.OrderResponse order = orders.get(position);
        holder.orderId.setText("Order #" + order.orderId + " - " + order.customerName);
        holder.total.setText(CurrencyFormatter.format(order.totalAmount) + " - " + order.paymentMethod);
        holder.address.setText(order.shippingAddress);
        holder.phone.setText(order.customerPhone != null && !order.customerPhone.isEmpty()
                ? "Contact: " + order.customerPhone : "Contact: not provided");

        String currentStatus = order.orderStatus != null ? order.orderStatus.trim().toUpperCase() : "PENDING";

        // Display current status on badge
        if ("CANCELLED".equals(currentStatus)) {
            holder.statusBadge.setText("Cancelled by customer");
            holder.statusBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
        } else if ("REJECTED".equals(currentStatus)) {
            holder.statusBadge.setText("REJECTED");
            holder.statusBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
        } else if ("DELIVERED".equals(currentStatus)) {
            holder.statusBadge.setText("DELIVERED");
            holder.statusBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
        } else if ("OUT_FOR_DELIVERY".equals(currentStatus)) {
            holder.statusBadge.setText("OUT_FOR_DELIVERY");
            holder.statusBadge.setBackgroundColor(Color.parseColor("#EF6C00"));
        } else {
            holder.statusBadge.setText(currentStatus);
            holder.statusBadge.setBackgroundColor(Color.parseColor("#1565C0"));
        }

        // Allowed transitions for dropdown
        List<String> nextStatuses = getAllowedTransitions(currentStatus);

        if (nextStatuses.isEmpty()) {
            holder.statusSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, new String[0]));
            holder.statusSpinner.setVisibility(View.GONE);
            holder.statusMessage.setVisibility(View.VISIBLE);

            if ("CANCELLED".equals(currentStatus)) {
                holder.statusMessage.setText("Cancelled by customer");
            } else if ("REJECTED".equals(currentStatus)) {
                holder.statusMessage.setText("Order Rejected");
            } else if ("DELIVERED".equals(currentStatus)) {
                holder.statusMessage.setText("Order Delivered");
            } else {
                holder.statusMessage.setText(currentStatus);
            }

            holder.updateButton.setEnabled(false);
            holder.updateButton.setAlpha(0.5f);
        } else {
            holder.statusSpinner.setVisibility(View.VISIBLE);
            holder.statusMessage.setVisibility(View.GONE);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, nextStatuses);
            holder.statusSpinner.setAdapter(spinnerAdapter);

            holder.updateButton.setEnabled(true);
            holder.updateButton.setAlpha(1.0f);
        }

        holder.updateButton.setOnClickListener(v -> {
            String selected = null;
            if (holder.statusSpinner.getVisibility() == View.VISIBLE && holder.statusSpinner.getSelectedItem() != null) {
                selected = holder.statusSpinner.getSelectedItem().toString();
            }
            listener.onUpdateStatus(order, selected);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, total, address, phone;
        TextView statusBadge, statusMessage;
        Spinner statusSpinner;
        Button updateButton;

        ViewHolder(View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.foOrderId);
            statusBadge = itemView.findViewById(R.id.foStatusBadge);
            total = itemView.findViewById(R.id.foTotal);
            address = itemView.findViewById(R.id.foAddress);
            phone = itemView.findViewById(R.id.foPhone);
            statusSpinner = itemView.findViewById(R.id.foStatusSpinner);
            statusMessage = itemView.findViewById(R.id.foStatusMessage);
            updateButton = itemView.findViewById(R.id.foUpdateButton);
        }
    }
}
