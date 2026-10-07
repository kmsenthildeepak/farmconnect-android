package com.farmconnect.android.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.TextView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.AdminUserAdapter;
import com.farmconnect.android.model.UserProfile;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin "Total Users" screen: every user (customers + farmers), from GET /api/admin/users. */
public class AdminUsersActivity extends AppCompatActivity {

    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyState;
    private AdminUserAdapter adapter;
    private final List<UserProfile> users = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Total Users");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        list = findViewById(R.id.listRecyclerView);
        swipeRefresh = findViewById(R.id.listSwipeRefresh);
        emptyState = findViewById(R.id.listEmptyState);
        emptyState.setText("No users found.");

        apiService = ApiClient.getApiService(this);

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(this, users);
        list.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        apiService.allUsers().enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(Call<List<UserProfile>> call, Response<List<UserProfile>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AdminUsersActivity.this, "Could not load users", Toast.LENGTH_SHORT).show();
                    return;
                }
                users.clear();
                users.addAll(response.body());
                adapter.notifyDataSetChanged();
                boolean empty = users.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<UserProfile>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminUsersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
