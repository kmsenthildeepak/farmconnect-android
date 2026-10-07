package com.farmconnect.android.ui.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.OrderAdapter;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity
        extends AppCompatActivity {

    private RecyclerView orderList;

    private TextView emptyText;

    private OrderAdapter adapter;

    private ApiService apiService;

    private final List<OrderModels.OrderResponse> orders =
            new ArrayList<>();

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_order_history
        );

        // ---------------------------------------------------------
        // Toolbar
        // ---------------------------------------------------------

        Toolbar toolbar =
                findViewById(
                        R.id.toolbar
                );

        setSupportActionBar(
                toolbar
        );

        if (getSupportActionBar() != null) {

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(
                            true
                    );
        }

        toolbar.setNavigationOnClickListener(
                v -> finish()
        );

        // ---------------------------------------------------------
        // Views
        // ---------------------------------------------------------

        orderList =
                findViewById(
                        R.id.orderList
                );

        emptyText =
                findViewById(
                        R.id.emptyOrdersText
                );

        // ---------------------------------------------------------
        // API
        // ---------------------------------------------------------

        apiService =
                ApiClient.getApiService(
                        this
                );

        // ---------------------------------------------------------
        // RecyclerView
        // ---------------------------------------------------------

        orderList.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new OrderAdapter(
                        this,
                        orders,
                        this::cancelOrder
                );

        orderList.setAdapter(
                adapter
        );

        // ---------------------------------------------------------
        // Initial state
        // ---------------------------------------------------------

        showMessage(
                "Loading your orders..."
        );
    }

    @Override
    protected void onResume() {

        super.onResume();

        loadOrders();
    }

    // =============================================================
    // LOAD ORDERS
    // =============================================================

    private void loadOrders() {

        showMessage(
                "Loading your orders..."
        );

        apiService
                .myOrders()
                .enqueue(
                        new Callback<List<OrderModels.OrderResponse>>() {

                            @Override
                            public void onResponse(
                                    Call<List<OrderModels.OrderResponse>> call,
                                    Response<List<OrderModels.OrderResponse>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    orders.clear();

                                    orders.addAll(
                                            response.body()
                                    );

                                    adapter.notifyDataSetChanged();

                                    if (orders.isEmpty()) {

                                        showMessage(
                                                "No orders yet"
                                        );

                                    } else {

                                        showOrders();
                                    }

                                } else {

                                    orders.clear();

                                    adapter.notifyDataSetChanged();

                                    showMessage(
                                            getErrorMessage(
                                                    response
                                            )
                                    );

                                    Toast.makeText(
                                            OrderHistoryActivity.this,
                                            getErrorMessage(
                                                    response
                                            ),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<OrderModels.OrderResponse>> call,
                                    Throwable t
                            ) {

                                orders.clear();

                                adapter.notifyDataSetChanged();

                                showMessage(
                                        "Unable to load orders.\nPlease check your connection and try again."
                                );

                                Toast.makeText(
                                        OrderHistoryActivity.this,
                                        "Network error: "
                                                + safeMessage(t),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // CANCEL ORDER
    // =============================================================

    private void cancelOrder(OrderModels.OrderResponse order) {

        apiService
                .cancelOrder(order.orderId)
                .enqueue(
                        new Callback<OrderModels.OrderResponse>() {

                            @Override
                            public void onResponse(
                                    Call<OrderModels.OrderResponse> call,
                                    Response<OrderModels.OrderResponse> response
                            ) {

                                if (response.isSuccessful()) {

                                    Toast.makeText(
                                            OrderHistoryActivity.this,
                                            "Order cancelled",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    loadOrders();

                                } else {

                                    Toast.makeText(
                                            OrderHistoryActivity.this,
                                            getErrorMessage(response),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<OrderModels.OrderResponse> call,
                                    Throwable t
                            ) {

                                Toast.makeText(
                                        OrderHistoryActivity.this,
                                        "Network error: " + safeMessage(t),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // SHOW ORDERS
    // =============================================================

    private void showOrders() {

        emptyText.setVisibility(
                View.GONE
        );

        orderList.setVisibility(
                View.VISIBLE
        );
    }

    // =============================================================
    // SHOW EMPTY / ERROR / LOADING
    // =============================================================

    private void showMessage(
            String message
    ) {

        emptyText.setText(
                message
        );

        emptyText.setVisibility(
                View.VISIBLE
        );

        orderList.setVisibility(
                View.GONE
        );
    }

    // =============================================================
    // ERROR
    // =============================================================

    private String getErrorMessage(
            Response<?> response
    ) {

        if (response == null) {

            return "Could not load your orders.";
        }

        switch (response.code()) {

            case 401:
                return "Your session has expired. Please login again.";

            case 403:
                return "You are not allowed to view orders.";

            case 404:
                return "Order history was not found.";

            case 500:
                return "Server error while loading orders.";

            default:
                return "Could not load orders. HTTP "
                        + response.code();
        }
    }

    private String safeMessage(
            Throwable t
    ) {

        if (t == null
                || t.getMessage() == null
                || t.getMessage().trim().isEmpty()) {

            return "Unknown network error";
        }

        return t.getMessage();
    }
}