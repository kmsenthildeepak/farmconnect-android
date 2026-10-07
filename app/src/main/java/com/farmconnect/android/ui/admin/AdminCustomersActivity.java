package com.farmconnect.android.ui.admin;

import android.app.AlertDialog;
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
import com.farmconnect.android.adapter.AdminCustomerAdapter;
import com.farmconnect.android.model.UserProfile;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Admin Customers screen - dynamic list from GET /api/admin/customers with
 * a confirm-then-block action (DELETE /api/admin/customers/{userId}). See
 * AdminServiceImpl#blockCustomer on the backend - existing orders/reviews
 * are left in place, only login is blocked.
 */
public class AdminCustomersActivity extends AppCompatActivity {

    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyState;
    private AdminCustomerAdapter adapter;
    private final List<UserProfile> customers = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Customers");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        list = findViewById(R.id.listRecyclerView);
        swipeRefresh = findViewById(R.id.listSwipeRefresh);
        emptyState = findViewById(R.id.listEmptyState);
        emptyState.setText("No customers found.");

        apiService = ApiClient.getApiService(this);

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminCustomerAdapter(this, customers, new AdminCustomerAdapter.OnActionListener() {
            @Override
            public void onBlock(UserProfile customer) {
                confirmBlock(customer);
            }

            @Override
            public void onUnblock(UserProfile customer) {
                confirmUnblock(customer);
            }
        });
        list.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void confirmBlock(UserProfile customer) {
        new AlertDialog.Builder(this)
                .setTitle("Block customer")
                .setMessage("Are you sure you want to block " + customer.name
                        + "? They will no longer be able to log in. Their existing order and review history is kept.")
                .setPositiveButton("Block", (dialog, which) -> block(customer))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void block(UserProfile customer) {
        apiService.blockCustomer(customer.userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminCustomersActivity.this, "Customer blocked", Toast.LENGTH_SHORT).show();
                    load();
                } else {
                    Toast.makeText(AdminCustomersActivity.this, "Could not block customer", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminCustomersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmUnblock(UserProfile customer) {
        new AlertDialog.Builder(this)
                .setTitle("Unblock customer")
                .setMessage("Are you sure you want to unblock " + customer.name
                        + "? They will be able to log in again.")
                .setPositiveButton("Unblock", (dialog, which) -> unblock(customer))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unblock(UserProfile customer) {
        apiService.unblockCustomer(customer.userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminCustomersActivity.this, "Customer unblocked", Toast.LENGTH_SHORT).show();
                    load();
                } else {
                    Toast.makeText(AdminCustomersActivity.this, "Could not unblock customer", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminCustomersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        apiService.allCustomers().enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(Call<List<UserProfile>> call, Response<List<UserProfile>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AdminCustomersActivity.this, "Could not load customers", Toast.LENGTH_SHORT).show();
                    return;
                }
                customers.clear();
                customers.addAll(response.body());
                adapter.notifyDataSetChanged();
                boolean empty = customers.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<UserProfile>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminCustomersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
