package com.farmconnect.android.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.ui.customer.OrderTrackingActivity;
import com.farmconnect.android.util.CurrencyFormatter;

import java.util.Arrays;
import java.util.List;

public class OrderAdapter
        extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    /** Mirrors OrderServiceImpl#CUSTOMER_CANCELLABLE_STATUSES on the backend. */
    private static final List<String> CANCELLABLE_STATUSES = Arrays.asList("PENDING", "ACCEPTED");

    public interface OnCancelListener {
        void onCancel(OrderModels.OrderResponse order);
    }

    private final Context context;

    private final List<OrderModels.OrderResponse> orders;
    private final OnCancelListener cancelListener;

    public OrderAdapter(
            Context context,
            List<OrderModels.OrderResponse> orders,
            OnCancelListener cancelListener
    ) {

        this.context =
                context;

        this.orders =
                orders;

        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(context)
                        .inflate(
                                R.layout.item_order,
                                parent,
                                false
                        );

        return new ViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        OrderModels.OrderResponse order =
                orders.get(position);

        // ---------------------------------------------------------
        // Order ID
        // ---------------------------------------------------------

        holder.orderId.setText(
                "Order #"
                        + order.orderId
        );

        // ---------------------------------------------------------
        // Status
        // ---------------------------------------------------------

        String status =
                order.orderStatus;

        if (status == null
                || status.trim().isEmpty()) {

            status =
                    "UNKNOWN";
        }

        holder.status.setText(
                "Status: "
                        + status
        );

        // ---------------------------------------------------------
        // Total
        // ---------------------------------------------------------

        int itemCount =
                order.items != null
                        ? order.items.size()
                        : 0;

        holder.total.setText(
                CurrencyFormatter.format(order.totalAmount)
                        + " - "
                        + itemCount
                        + " item(s)"
        );

        // ---------------------------------------------------------
        // Date
        // ---------------------------------------------------------

        holder.date.setText(
                formatDate(
                        order.createdAt
                )
        );

        // ---------------------------------------------------------
        // Cancel Order
        // ---------------------------------------------------------

        boolean cancellable = CANCELLABLE_STATUSES.contains(status);
        holder.cancelButton.setVisibility(cancellable ? View.VISIBLE : View.GONE);
        holder.cancelButton.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Cancel order")
                    .setMessage("Are you sure you want to cancel this order?")
                    .setPositiveButton("Yes, cancel", (dialog, which) -> {
                        if (cancelListener != null) {
                            cancelListener.onCancel(order);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // ---------------------------------------------------------
        // Open tracking
        // ---------------------------------------------------------

        holder.itemView.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    context,
                                    OrderTrackingActivity.class
                            );

                    intent.putExtra(
                            "orderId",
                            order.orderId
                    );

                    context.startActivity(
                            intent
                    );
                }
        );
    }

    @Override
    public int getItemCount() {

        return orders.size();
    }

    private String formatDate(
            String createdAt
    ) {

        if (createdAt == null
                || createdAt.trim().isEmpty()) {

            return "Date unavailable";
        }

        String value =
                createdAt.trim();

        /*
         * Existing backend value is treated as an ISO-like
         * date/time string.
         *
         * Example:
         * 2026-08-22T12:34:56
         *
         * Display:
         * 2026-08-22
         */

        if (value.length() >= 10) {

            return "Date: "
                    + value.substring(
                    0,
                    10
            );
        }

        return "Date: "
                + value;
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView orderId;
        TextView status;
        TextView total;
        TextView date;
        Button cancelButton;

        ViewHolder(
                View itemView
        ) {

            super(itemView);

            orderId =
                    itemView.findViewById(
                            R.id.orderIdText
                    );

            status =
                    itemView.findViewById(
                            R.id.orderStatusText
                    );

            total =
                    itemView.findViewById(
                            R.id.orderTotalText
                    );

            date =
                    itemView.findViewById(
                            R.id.orderDateText
                    );

            cancelButton =
                    itemView.findViewById(
                            R.id.orderCancelButton
                    );
        }
    }
}