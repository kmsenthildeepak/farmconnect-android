package com.farmconnect.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.model.NotificationModels;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {
    public interface Listener { void onClick(long id); }
    private List<NotificationModels.NotificationResponse> data;
    private final Listener listener;
    public NotificationAdapter(List<NotificationModels.NotificationResponse> data, Listener listener) { this.data = data; this.listener = listener; }
    public void update(List<NotificationModels.NotificationResponse> data) { this.data = data; notifyDataSetChanged(); }
    public NotificationModels.NotificationResponse getItem(int position) { return data.get(position); }
    public void removeAt(int position) { data.remove(position); notifyItemRemoved(position); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p, int v) { return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false)); }
    @Override public void onBindViewHolder(@NonNull Holder h, int i) {
        NotificationModels.NotificationResponse n = data.get(i);
        h.title.setText(n.title);
        h.message.setText(n.message);
        h.time.setText(n.createdAt == null ? "" : n.createdAt.replace('T', ' '));
        h.itemView.setAlpha(n.isRead ? 0.65f : 1f);
        h.itemView.setOnClickListener(v -> listener.onClick(n.notificationId));
    }
    @Override public int getItemCount() { return data.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        Holder(View v) { super(v); title=v.findViewById(R.id.notificationTitle); message=v.findViewById(R.id.notificationMessage); time=v.findViewById(R.id.notificationTime); }
    }
}
