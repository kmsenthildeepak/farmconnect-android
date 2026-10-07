package com.farmconnect.android.ui.customer;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.farmconnect.android.R;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderTrackingActivity extends AppCompatActivity {

    // Mirrors Order.OrderStatus on the backend. REJECTED/CANCELLED are shown
    // separately since they break out of the normal forward-only flow.
    private static final List<String> FLOW = Arrays.asList(
            "PENDING", "ACCEPTED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED");

    private TextView orderIdText, totalText, addressText, paymentText;
    private LinearLayout statusStepper, itemsContainer;
    private ApiService apiService;
    private long orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        orderIdText = findViewById(R.id.orderIdText);
        totalText = findViewById(R.id.totalText);
        addressText = findViewById(R.id.addressText);
        paymentText = findViewById(R.id.paymentText);
        statusStepper = findViewById(R.id.statusStepper);
        itemsContainer = findViewById(R.id.itemsContainer);

        apiService = ApiClient.getApiService(this);
        orderId = getIntent().getLongExtra("orderId", -1);

        loadOrder();
    }

    private void loadOrder() {
        apiService.getOrder(orderId).enqueue(new Callback<OrderModels.OrderResponse>() {
            @Override
            public void onResponse(Call<OrderModels.OrderResponse> call, Response<OrderModels.OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bind(response.body());
                } else {
                    Toast.makeText(OrderTrackingActivity.this, "Could not load order", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderModels.OrderResponse> call, Throwable t) {
                Toast.makeText(OrderTrackingActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bind(OrderModels.OrderResponse order) {
        orderIdText.setText("Order #" + order.orderId);
        totalText.setText(com.farmconnect.android.util.CurrencyFormatter.format(order.totalAmount));
        addressText.setText(order.shippingAddress);
        paymentText.setText(order.paymentMethod + " - " + order.paymentStatus);

        renderStepper(order.orderStatus);
        renderItems(order);
    }

    private void renderStepper(String currentStatus) {
        statusStepper.removeAllViews();

        if (currentStatus.equals("REJECTED") || currentStatus.equals("CANCELLED")) {
            TextView tv = new TextView(this);
            tv.setText("This order was " + currentStatus.toLowerCase());
            tv.setTextColor(getResources().getColor(R.color.error_red));
            tv.setPadding(0, 16, 0, 16);
            statusStepper.addView(tv);
            return;
        }

        int currentIndex = FLOW.indexOf(currentStatus);
        for (int i = 0; i < FLOW.size(); i++) {
            TextView tv = new TextView(this);
            String label = FLOW.get(i).replace("_", " ");
            boolean reached = i <= currentIndex;
            tv.setText((reached ? "✓ " : "○ ") + label);
            tv.setTextColor(getResources().getColor(reached ? R.color.farm_green : R.color.text_secondary));
            tv.setTextSize(15);
            tv.setPadding(0, 12, 0, 12);
            statusStepper.addView(tv);
        }
    }

    private void renderItems(OrderModels.OrderResponse order) {
        itemsContainer.removeAllViews();
        if (order.items == null) return;

        for (OrderModels.OrderItem item : order.items) {
            TextView tv = new TextView(this);
            tv.setText(item.productName + " x" + item.quantity + " - "
                    + com.farmconnect.android.util.CurrencyFormatter.format(item.subtotal)
                    + " - " + item.farmName);
            tv.setTextColor(getResources().getColor(R.color.text_primary));
            tv.setPadding(0, 8, 0, 8);
            itemsContainer.addView(tv);
        }
    }
}
