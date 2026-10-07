package com.farmconnect.android.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.AdminProductAdapter;
import com.farmconnect.android.model.Dashboard;
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

/**
 * Admin Home: four live stat cards (Sales-Report-style) + a farmer search
 * section. Orders/Pending-Verifications/Revenue and the Verify
 * Farmers/Notifications/Logout buttons have all moved to the sidebar -
 * they still work, they're just not primary Home content anymore.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView totalUsers, totalFarmers, totalCustomers, totalProducts;
    private EditText farmerIdInput, farmerEmailInput;
    private View farmerResultCard;
    private TextView farmerResultDetails, farmerSearchEmptyState, farmerProductsLabel;
    private RecyclerView farmerProductsGrid;
    private AdminProductAdapter farmerProductsAdapter;
    private final List<Product> farmerProducts = new ArrayList<>();
    private NestedScrollView dashboardScrollView;

    private DrawerLayout drawerLayout;
    private ApiService apiService;
    private SessionManager sessionManager;
    private TextView notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        totalUsers = findViewById(R.id.statTotalUsers);
        totalFarmers = findViewById(R.id.statTotalFarmers);
        totalCustomers = findViewById(R.id.statTotalCustomers);
        totalProducts = findViewById(R.id.statTotalProducts);
        drawerLayout = findViewById(R.id.drawerLayout);

        farmerIdInput = findViewById(R.id.searchFarmerIdInput);
        farmerEmailInput = findViewById(R.id.searchFarmerEmailInput);
        Button searchButton = findViewById(R.id.searchFarmerButton);
        farmerResultCard = findViewById(R.id.farmerResultCard);
        farmerResultDetails = findViewById(R.id.farmerResultDetails);
        farmerSearchEmptyState = findViewById(R.id.farmerSearchEmptyState);
        farmerProductsLabel = findViewById(R.id.farmerProductsLabel);
        farmerProductsGrid = findViewById(R.id.farmerProductsGrid);
        dashboardScrollView = findViewById(R.id.dashboardScrollView);

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        farmerProductsGrid.setLayoutManager(new GridLayoutManager(this, 1));
        farmerProductsAdapter = new AdminProductAdapter(this, farmerProducts);
        farmerProductsGrid.setAdapter(farmerProductsAdapter);

        setupDrawer(toolbar);

        // Dashboard stat cards - each opens its dynamic list screen.
        findViewById(R.id.cardTotalUsers).setOnClickListener(v -> startActivity(new Intent(this, AdminUsersActivity.class)));
        findViewById(R.id.cardTotalFarmers).setOnClickListener(v -> startActivity(new Intent(this, AdminFarmersActivity.class)));
        findViewById(R.id.cardTotalCustomers).setOnClickListener(v -> startActivity(new Intent(this, AdminCustomersActivity.class)));
        findViewById(R.id.cardTotalProducts).setOnClickListener(v -> startActivity(new Intent(this, AdminProductsActivity.class)));

        searchButton.setOnClickListener(v -> searchFarmer());
    }

    /**
     * Sidebar wiring, same pattern as CustomerHomeActivity/FarmerHomeActivity.
     * Every item opens a distinct screen - none of them alias Home. Verify
     * Farmers, Notifications and Logout now live only here (moved off the
     * Home content per spec).
     */
    private void setupDrawer(Toolbar toolbar) {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        findViewById(R.id.navTotalUsers).setOnClickListener(v -> openDrawerDestination(AdminUsersActivity.class));
        findViewById(R.id.navFarmers).setOnClickListener(v -> openDrawerDestination(AdminFarmersActivity.class));
        findViewById(R.id.navCustomers).setOnClickListener(v -> openDrawerDestination(AdminCustomersActivity.class));
        findViewById(R.id.navProducts).setOnClickListener(v -> openDrawerDestination(AdminProductsActivity.class));
        findViewById(R.id.navOrders).setOnClickListener(v -> openDrawerDestination(AdminOrdersActivity.class));
        findViewById(R.id.navPendingVerifications).setOnClickListener(v -> openDrawerDestination(FarmerVerificationActivity.class));
        findViewById(R.id.navNotifications).setOnClickListener(v -> openDrawerDestination(com.farmconnect.android.ui.common.NotificationsActivity.class));
        findViewById(R.id.navLogout).setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void openDrawerDestination(Class<? extends AppCompatActivity> activity) {
        drawerLayout.closeDrawer(Gravity.START);
        startActivity(new Intent(this, activity));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin_dashboard, menu);

        MenuItem notificationsItem = menu.findItem(R.id.action_notifications);
        if (notificationsItem != null) {
            View bellView = notificationsItem.getActionView();
            if (bellView != null) {
                bellView.findViewById(R.id.notificationBellButton).setOnClickListener(v ->
                        startActivity(new Intent(this, com.farmconnect.android.ui.common.NotificationsActivity.class)));
                notificationBadge = bellView.findViewById(R.id.notificationBadgeCount);
                loadUnreadNotificationCount();
            }
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

    private void loadUnreadNotificationCount() {
        if (notificationBadge == null) {
            return;
        }
        apiService.unreadNotificationCount().enqueue(new Callback<java.util.Map<String, Long>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Long>> call, Response<java.util.Map<String, Long>> response) {
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
            public void onFailure(Call<java.util.Map<String, Long>> call, Throwable t) {
                // Silent - badge just stays at its last known state.
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
        loadUnreadNotificationCount();
    }

    private void loadDashboard() {
        apiService.getDashboard().enqueue(new Callback<Dashboard>() {
            @Override
            public void onResponse(Call<Dashboard> call, Response<Dashboard> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bind(response.body());
                }
            }

            @Override
            public void onFailure(Call<Dashboard> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bind(Dashboard d) {
        totalUsers.setText(String.valueOf(d.totalUsers));
        totalFarmers.setText(String.valueOf(d.totalFarmers));
        totalCustomers.setText(String.valueOf(d.totalCustomers));
        totalProducts.setText(String.valueOf(d.totalProducts));
    }

    // =========================================================
    // FARMER SEARCH
    // =========================================================

    private void searchFarmer() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
        if (farmerIdInput != null) {
            farmerIdInput.clearFocus();
        }
        if (farmerEmailInput != null) {
            farmerEmailInput.clearFocus();
        }

        String idText = farmerIdInput.getText().toString().trim();
        String email = farmerEmailInput.getText().toString().trim();

        if (TextUtils.isEmpty(idText) && TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter a Farmer ID or email", Toast.LENGTH_SHORT).show();
            return;
        }

        Long farmerId = null;
        if (!TextUtils.isEmpty(idText)) {
            try {
                farmerId = Long.parseLong(idText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Farmer ID must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        apiService.searchFarmer(farmerId, TextUtils.isEmpty(email) ? null : email).enqueue(new Callback<Farmer>() {
            @Override
            public void onResponse(Call<Farmer> call, Response<Farmer> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    farmerResultCard.setVisibility(View.GONE);
                    farmerProductsLabel.setVisibility(View.GONE);
                    farmerProducts.clear();
                    farmerProductsAdapter.notifyDataSetChanged();
                    farmerSearchEmptyState.setVisibility(View.VISIBLE);
                    farmerSearchEmptyState.setText(response.code() == 404 ? "Farmer not found" : "Could not search for farmer");
                    farmerProductsGrid.post(() -> {
                        farmerProductsGrid.requestLayout();
                        if (dashboardScrollView != null) {
                            dashboardScrollView.requestLayout();
                        }
                    });
                    return;
                }
                farmerSearchEmptyState.setVisibility(View.GONE);
                showFarmer(response.body());
            }

            @Override
            public void onFailure(Call<Farmer> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showFarmer(Farmer f) {
        farmerResultCard.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder();
        sb.append("Farmer ID: ").append(f.farmerId).append("\n");
        sb.append("Name: ").append(f.farmerName != null ? f.farmerName : "-").append("\n");
        sb.append("Email: ").append(f.email != null ? f.email : "-").append("\n");
        sb.append("Phone: ").append(f.phone != null ? f.phone : "-").append("\n");
        sb.append("Farm name: ").append(f.farmName != null ? f.farmName : "-").append("\n");
        sb.append("Farm location: ").append(f.farmLocation != null ? f.farmLocation : "-").append("\n");
        sb.append("Address: ").append(f.farmAddress != null ? f.farmAddress : "-").append("\n");
        sb.append("Verification status: ").append(f.verificationStatus != null ? f.verificationStatus : "-").append("\n");
        sb.append("Account status: ").append(f.isActive ? "Active" : "Blocked");
        farmerResultDetails.setText(sb.toString());

        loadFarmerProducts(f.farmerId);
    }

    private void loadFarmerProducts(long farmerId) {
        apiService.farmerProducts(farmerId).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                farmerProducts.clear();
                if (response.isSuccessful() && response.body() != null) {
                    farmerProducts.addAll(response.body());
                }
                farmerProductsAdapter.notifyDataSetChanged();
                farmerProductsLabel.setVisibility(View.VISIBLE);
                farmerProductsLabel.setText(farmerProducts.isEmpty() ? "No products found." : "Products");

                farmerProductsGrid.post(() -> {
                    farmerProductsGrid.requestLayout();
                    if (dashboardScrollView != null) {
                        dashboardScrollView.requestLayout();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
