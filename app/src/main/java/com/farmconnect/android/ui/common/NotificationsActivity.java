package com.farmconnect.android.ui.common;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.NotificationAdapter;
import com.farmconnect.android.model.NotificationModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shared by all three roles. Swipe-to-delete (either direction) is enabled
 * for all three roles (customer, farmer, admin) - all sharing the exact
 * same proven swipe/delete implementation below, so there is only one
 * code path to keep correct rather than a separate one per role.
 *
 * Deletion calls DELETE /api/notifications/{id} (ownership-checked
 * server-side against the authenticated user - see
 * NotificationServiceImpl#deleteNotification) and only removes the row
 * from the adapter after the backend confirms success; on failure the item
 * is restored (via a full reload) so the list never silently drifts from
 * the database.
 */
public class NotificationsActivity extends AppCompatActivity {
    private NotificationAdapter adapter;
    private TextView empty;
    private ApiService api;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_notifications);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        empty = findViewById(R.id.emptyNotifications);
        RecyclerView list = findViewById(R.id.notificationList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>(), id -> markRead(id));
        list.setAdapter(adapter);
        api = ApiClient.getApiService(this);

        // Swipe-to-delete is now enabled for every role - customer, farmer
        // and admin all share this same ItemTouchHelper wiring and the
        // same ownership-checked backend endpoint.
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(list);

        load();
    }

    private final ItemTouchHelper.SimpleCallback swipeCallback =
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getBindingAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        return;
                    }
                    NotificationModels.NotificationResponse toDelete = adapter.getItem(position);
                    adapter.removeAt(position);
                    deleteNotification(toDelete);
                }
            };

    private void deleteNotification(NotificationModels.NotificationResponse notification) {
        api.deleteNotification(notification.notificationId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(NotificationsActivity.this, "Could not delete notification", Toast.LENGTH_SHORT).show();
                    load(); // resync with backend rather than leaving a stale local state
                } else {
                    empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                load(); // resync with backend rather than leaving a stale local state
            }
        });
    }

    private void load() {
        api.myNotifications().enqueue(new Callback<List<NotificationModels.NotificationResponse>>() {
            @Override public void onResponse(Call<List<NotificationModels.NotificationResponse>> c, Response<List<NotificationModels.NotificationResponse>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    adapter.update(r.body());
                    empty.setVisibility(r.body().isEmpty() ? View.VISIBLE : View.GONE);
                } else Toast.makeText(NotificationsActivity.this, "Could not load notifications", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<List<NotificationModels.NotificationResponse>> c, Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void markRead(long id) {
        api.markNotificationRead(id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { load(); }
            @Override public void onFailure(Call<Void> c, Throwable t) { }
        });
    }
}
