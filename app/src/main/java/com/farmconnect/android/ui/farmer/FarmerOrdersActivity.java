package com.farmconnect.android.ui.farmer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.FarmerOrderAdapter;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerOrdersActivity extends AppCompatActivity implements FarmerOrderAdapter.Listener {

    private RecyclerView orderList;
    private TextView emptyText;
    private FarmerOrderAdapter adapter;
    private ApiService apiService;
    private final List<OrderModels.OrderResponse> orders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_orders);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        orderList = findViewById(R.id.farmerOrderList);
        emptyText = findViewById(R.id.emptyFarmerOrdersText);

        apiService = ApiClient.getApiService(this);

        orderList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FarmerOrderAdapter(this, orders, this);
        orderList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        apiService.farmerOrders().enqueue(new Callback<List<OrderModels.OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderModels.OrderResponse>> call, Response<List<OrderModels.OrderResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orders.clear();
                    orders.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    emptyText.setVisibility(orders.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<OrderModels.OrderResponse>> call, Throwable t) {
                Toast.makeText(FarmerOrdersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onUpdateStatus(OrderModels.OrderResponse order, String newStatus) {
        if (order == null) return;
        String currentStatus = order.orderStatus != null ? order.orderStatus.trim().toUpperCase() : "";

        if ("REJECTED".equals(currentStatus)) {
            Toast.makeText(this, "After the order is Rejected. You could not update the status", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("DELIVERED".equals(currentStatus)) {
            Toast.makeText(this, "After the Order is Delivered. You could not update the status", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("CANCELLED".equals(currentStatus)) {
            Toast.makeText(this, "Cancelled by customer", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newStatus == null || newStatus.trim().isEmpty()) {
            Toast.makeText(this, "Please select a valid status", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!FarmerOrderAdapter.isValidTransition(currentStatus, newStatus)) {
            Toast.makeText(this, "Invalid status transition from " + currentStatus + " to " + newStatus, Toast.LENGTH_SHORT).show();
            return;
        }

        OrderModels.OrderStatusUpdateRequest req = new OrderModels.OrderStatusUpdateRequest(newStatus);
        apiService.updateOrderStatus(order.orderId, req).enqueue(new Callback<OrderModels.OrderResponse>() {
            @Override
            public void onResponse(Call<OrderModels.OrderResponse> call, Response<OrderModels.OrderResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FarmerOrdersActivity.this, "Order status updated", Toast.LENGTH_SHORT).show();
                    loadOrders();
                } else {
                    String errorMsg = "Could not update status";
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            org.json.JSONObject obj = new org.json.JSONObject(errorJson);
                            if (obj.has("message")) {
                                errorMsg = obj.getString("message");
                            }
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(FarmerOrdersActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderModels.OrderResponse> call, Throwable t) {
                Toast.makeText(FarmerOrdersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
