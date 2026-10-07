package com.farmconnect.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.model.UserProfile;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private final Context context;
    private final List<UserProfile> users;

    public AdminUserAdapter(Context context, List<UserProfile> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile u = users.get(position);
        holder.name.setText(u.name);
        holder.email.setText(u.email);
        holder.role.setText(u.role != null ? u.role : "-");
        holder.status.setText(u.isActive ? "Active" : "Blocked");
        holder.status.setTextColor(context.getResources()
                .getColor(u.isActive ? R.color.farm_green_light : R.color.error_red));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, email, role, status;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.auName);
            email = itemView.findViewById(R.id.auEmail);
            role = itemView.findViewById(R.id.auRole);
            status = itemView.findViewById(R.id.auStatus);
        }
    }
}
