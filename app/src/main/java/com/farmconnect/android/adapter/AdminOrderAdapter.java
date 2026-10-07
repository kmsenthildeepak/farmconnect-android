package com.farmconnect.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.util.CurrencyFormatter;

import java.util.List;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.ViewHolder> {

    private final Context context;
    private final List<OrderModels.OrderResponse> orders;

    public AdminOrderAdapter(Context context, List<OrderModels.OrderResponse> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_order, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderModels.OrderResponse o = orders.get(position);

        holder.orderId.setText("Order #" + o.orderId);
        holder.status.setText(o.orderStatus != null ? o.orderStatus : "-");
        holder.customer.setText(o.customerName + (o.customerPhone != null ? " - " + o.customerPhone : ""));
        holder.total.setText(CurrencyFormatter.format(o.totalAmount) + " - "
                + o.paymentMethod + " - " + o.paymentStatus);
        holder.address.setText(o.shippingAddress);
        holder.date.setText(o.createdAt != null ? o.createdAt.replace("T", " ") : "");
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, status, customer, total, address, date;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.aoOrderId);
            status = itemView.findViewById(R.id.aoStatus);
            customer = itemView.findViewById(R.id.aoCustomer);
            total = itemView.findViewById(R.id.aoTotal);
            address = itemView.findViewById(R.id.aoAddress);
            date = itemView.findViewById(R.id.aoDate);
        }
    }
}
