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
import com.farmconnect.android.adapter.AdminProductAdapter;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin Products screen - every product regardless of availability, from GET /api/admin/products. */
public class AdminProductsActivity extends AppCompatActivity {

    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyState;
    private AdminProductAdapter adapter;
    private final List<Product> products = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Products");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        list = findViewById(R.id.listRecyclerView);
        swipeRefresh = findViewById(R.id.listSwipeRefresh);
        emptyState = findViewById(R.id.listEmptyState);
        emptyState.setText("No products found.");

        apiService = ApiClient.getApiService(this);

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProductAdapter(this, products);
        list.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        apiService.adminAllProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AdminProductsActivity.this, "Could not load products", Toast.LENGTH_SHORT).show();
                    return;
                }
                products.clear();
                products.addAll(response.body());
                adapter.notifyDataSetChanged();
                boolean empty = products.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminProductsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
