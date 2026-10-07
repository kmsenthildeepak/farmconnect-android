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

public class FarmerVerificationAdapter extends RecyclerView.Adapter<FarmerVerificationAdapter.ViewHolder> {

    public interface Listener {
        void onApprove(Farmer farmer);
        void onReject(Farmer farmer);
    }

    private final Context context;
    private final List<Farmer> farmers;
    private final Listener listener;

    public FarmerVerificationAdapter(Context context, List<Farmer> farmers, Listener listener) {
        this.context = context;
        this.farmers = farmers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_farmer_verification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Farmer f = farmers.get(position);
        holder.name.setText(f.farmerName);
        holder.farmName.setText(f.farmName);
        holder.phone.setText(f.phone != null ? f.phone : "");

        holder.approveButton.setOnClickListener(v -> listener.onApprove(f));
        holder.rejectButton.setOnClickListener(v -> listener.onReject(f));
    }

    @Override
    public int getItemCount() {
        return farmers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, farmName, phone;
        Button approveButton, rejectButton;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.fvName);
            farmName = itemView.findViewById(R.id.fvFarmName);
            phone = itemView.findViewById(R.id.fvPhone);
            approveButton = itemView.findViewById(R.id.fvApproveButton);
            rejectButton = itemView.findViewById(R.id.fvRejectButton);
        }
    }
}
