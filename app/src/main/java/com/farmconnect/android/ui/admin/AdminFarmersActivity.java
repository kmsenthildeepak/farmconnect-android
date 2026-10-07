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
import com.farmconnect.android.adapter.AdminFarmerAdapter;
import com.farmconnect.android.model.Farmer;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Admin Farmers screen - dynamic list from GET /api/admin/farmers with a
 * confirm-then-block action (DELETE /api/admin/farmers/{farmerId}). See
 * AdminServiceImpl#blockFarmer on the backend for why this deactivates +
 * hides products rather than hard-deleting (preserves existing order
 * history referencing this farmer's products).
 */
public class AdminFarmersActivity extends AppCompatActivity {

    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyState;
    private AdminFarmerAdapter adapter;
    private final List<Farmer> farmers = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Farmers");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        list = findViewById(R.id.listRecyclerView);
        swipeRefresh = findViewById(R.id.listSwipeRefresh);
        emptyState = findViewById(R.id.listEmptyState);
        emptyState.setText("No farmers found.");

        apiService = ApiClient.getApiService(this);

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminFarmerAdapter(this, farmers, new AdminFarmerAdapter.OnActionListener() {
            @Override
            public void onBlock(Farmer farmer) {
                confirmBlock(farmer);
            }

            @Override
            public void onUnblock(Farmer farmer) {
                confirmUnblock(farmer);
            }
        });
        list.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void confirmBlock(Farmer farmer) {
        new AlertDialog.Builder(this)
                .setTitle("Block farmer")
                .setMessage("Are you sure you want to block " + farmer.farmerName
                        + "? They will no longer be able to log in and their products will be hidden from customers. Existing orders are kept.")
                .setPositiveButton("Block", (dialog, which) -> block(farmer))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void block(Farmer farmer) {
        apiService.blockFarmer(farmer.farmerId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminFarmersActivity.this, "Farmer blocked", Toast.LENGTH_SHORT).show();
                    load();
                } else {
                    Toast.makeText(AdminFarmersActivity.this, "Could not block farmer", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminFarmersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmUnblock(Farmer farmer) {
        new AlertDialog.Builder(this)
                .setTitle("Unblock farmer")
                .setMessage("Are you sure you want to unblock " + farmer.farmerName
                        + "? They will be able to log in and sell products again.")
                .setPositiveButton("Unblock", (dialog, which) -> unblock(farmer))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unblock(Farmer farmer) {
        apiService.unblockFarmer(farmer.farmerId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminFarmersActivity.this, "Farmer unblocked", Toast.LENGTH_SHORT).show();
                    load();
                } else {
                    Toast.makeText(AdminFarmersActivity.this, "Could not unblock farmer", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminFarmersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        apiService.allFarmers().enqueue(new Callback<List<Farmer>>() {
            @Override
            public void onResponse(Call<List<Farmer>> call, Response<List<Farmer>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AdminFarmersActivity.this, "Could not load farmers", Toast.LENGTH_SHORT).show();
                    return;
                }
                farmers.clear();
                farmers.addAll(response.body());
                adapter.notifyDataSetChanged();
                boolean empty = farmers.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<Farmer>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminFarmersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
