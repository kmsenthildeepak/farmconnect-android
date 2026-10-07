package com.farmconnect.android.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.FarmerHomeProductAdapter;
import com.farmconnect.android.model.Farmer;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;
import com.farmconnect.android.ui.auth.LoginActivity;
import com.farmconnect.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerHomeActivity extends AppCompatActivity {

    private TextView greeting;
    private TextView verificationBanner;
    private TextView emptyState;
    private TextView drawerFarmerName;
    private TextView notificationBadge;
    private DrawerLayout drawerLayout;
    private RecyclerView productGrid;
    private SwipeRefreshLayout swipeRefresh;
    private FarmerHomeProductAdapter adapter;
    private final List<Product> myProducts = new ArrayList<>();

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        greeting = findViewById(R.id.greeting);
        verificationBanner = findViewById(R.id.verificationBanner);
        emptyState = findViewById(R.id.emptyState);
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerFarmerName = findViewById(R.id.drawerFarmerName);
        productGrid = findViewById(R.id.farmerProductGrid);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        String farmerName = safeName(sessionManager.getName());
        greeting.setText("Welcome, " + farmerName);
        drawerFarmerName.setText(farmerName);

        productGrid.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new FarmerHomeProductAdapter(this, myProducts);
        productGrid.setAdapter(adapter);
        swipeRefresh.setOnRefreshListener(this::loadMyProducts);

        setupDrawer(toolbar);
    }

    private String safeName(String name) {
        if (name == null) {
            return "Farmer";
        }
        String cleaned = name.trim();
        return cleaned.isEmpty() ? "Farmer" : cleaned;
    }

    private void setupDrawer(Toolbar toolbar) {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        findViewById(R.id.drawerFarmerProfileRow).setOnClickListener(v ->
                openDestination(FarmerProfileActivity.class));

        findViewById(R.id.farmerNavHome).setOnClickListener(v ->
                drawerLayout.closeDrawer(Gravity.START));

        findViewById(R.id.farmerNavProducts).setOnClickListener(v ->
                openDestination(ManageProductsActivity.class));

        findViewById(R.id.farmerNavOrders).setOnClickListener(v ->
                openDestination(FarmerOrdersActivity.class));

        findViewById(R.id.farmerNavSales).setOnClickListener(v ->
                openDestination(SalesReportActivity.class));

        findViewById(R.id.farmerNavNotifications).setOnClickListener(v ->
                openDestination(com.farmconnect.android.ui.common.NotificationsActivity.class));

        findViewById(R.id.farmerNavLogout).setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void openDestination(Class<? extends AppCompatActivity> activity) {
        drawerLayout.closeDrawer(Gravity.START);
        startActivity(new Intent(this, activity));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVerificationStatus();
        loadMyProducts();
        loadUnreadNotificationCount();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_farmer_home, menu);
        MenuItem notificationsItem = menu.findItem(R.id.action_notifications);
        View bellView = notificationsItem.getActionView();
        if (bellView != null) {
            bellView.findViewById(R.id.notificationBellButton).setOnClickListener(v ->
                    startActivity(new Intent(this, com.farmconnect.android.ui.common.NotificationsActivity.class)));
            notificationBadge = bellView.findViewById(R.id.notificationBadgeCount);
            loadUnreadNotificationCount();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_notifications) {
            startActivity(new Intent(this, com.farmconnect.android.ui.common.NotificationsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Same pattern as CustomerHomeActivity: backend is the source of truth
     * (GET /api/notifications/unread-count, resolved from the authenticated
     * farmer's own token - a farmer can only ever see their own unread
     * count, never another farmer's).
     */
    private void loadUnreadNotificationCount() {
        if (notificationBadge == null) {
            return;
        }
        apiService.unreadNotificationCount().enqueue(new retrofit2.Callback<java.util.Map<String, Long>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Long>> call, retrofit2.Response<java.util.Map<String, Long>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                Long unread = response.body().get("unread");
                long count = unread == null ? 0 : unread;
                if (count <= 0) {
                    notificationBadge.setVisibility(View.GONE);
                } else {
                    notificationBadge.setVisibility(View.VISIBLE);
                    notificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Long>> call, Throwable t) {
                // Silent - badge just stays at its last known state.
            }
        });
    }

    /**
     * Farmer Home shows ONLY this farmer's own products. This calls
     * GET /api/products/mine, which the backend resolves from the
     * authenticated user's token (see ProductServiceImpl#myProducts) -
     * ownership is enforced server-side, not by filtering on the client.
     */
    private void loadMyProducts() {
        apiService.myProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                myProducts.clear();
                myProducts.addAll(response.body());
                adapter.notifyDataSetChanged();
                emptyState.setVisibility(myProducts.isEmpty() ? View.VISIBLE : View.GONE);
                productGrid.setVisibility(myProducts.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(FarmerHomeActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadVerificationStatus() {
        apiService.myFarmerProfile().enqueue(new Callback<Farmer>() {
            @Override
            public void onResponse(Call<Farmer> call, Response<Farmer> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                Farmer farmer = response.body();
                boolean verified = "VERIFIED".equals(farmer.verificationStatus);

                if (verified) {
                    verificationBanner.setVisibility(View.GONE);
                } else {
                    verificationBanner.setVisibility(View.VISIBLE);
                    verificationBanner.setText(
                            "REJECTED".equals(farmer.verificationStatus)
                                    ? "Your farmer account was rejected. Contact support for details."
                                    : "Your account is pending admin verification. You'll be able to add products once approved."
                    );
                }
            }

            @Override
            public void onFailure(Call<Farmer> call, Throwable t) {
                Toast.makeText(
                        FarmerHomeActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }
}
