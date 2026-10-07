package com.farmconnect.android.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.AdminOrderAdapter;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin Orders screen - every order in the system, from GET /api/admin/orders. */
public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyState;
    private AdminOrderAdapter adapter;
    private final List<OrderModels.OrderResponse> orders = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Orders");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        list = findViewById(R.id.listRecyclerView);
        swipeRefresh = findViewById(R.id.listSwipeRefresh);
        emptyState = findViewById(R.id.listEmptyState);
        emptyState.setText("No orders found.");

        apiService = ApiClient.getApiService(this);

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminOrderAdapter(this, orders);
        list.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        apiService.adminAllOrders().enqueue(new Callback<List<OrderModels.OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderModels.OrderResponse>> call, Response<List<OrderModels.OrderResponse>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AdminOrdersActivity.this, "Could not load orders", Toast.LENGTH_SHORT).show();
                    return;
                }
                orders.clear();
                orders.addAll(response.body());
                adapter.notifyDataSetChanged();
                boolean empty = orders.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<OrderModels.OrderResponse>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminOrdersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
