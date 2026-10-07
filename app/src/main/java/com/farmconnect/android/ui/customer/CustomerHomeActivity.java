package com.farmconnect.android.ui.customer;

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
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.ProductAdapter;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;
import com.farmconnect.android.util.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerHomeActivity extends AppCompatActivity {

    private RecyclerView productGrid;
    private ProductAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup categoryChips;
    private TextView greeting;
    private ApiService apiService;
    private SessionManager sessionManager;
    private DrawerLayout drawerLayout;
    private TextView notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        greeting = findViewById(R.id.greeting);
        productGrid = findViewById(R.id.productGrid);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        categoryChips = findViewById(R.id.categoryChips);
        drawerLayout = findViewById(R.id.drawerLayout);

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        greeting.setText("Hi, " + sessionManager.getName() + " 👋");

        setupDrawer(toolbar);

        productGrid.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(this, new ArrayList<>());
        productGrid.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadAllProducts);

        categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                loadAllProducts();
                return;
            }
            Chip chip = findViewById(checkedIds.get(0));
            String tag = chip.getTag() != null ? chip.getTag().toString() : "";
            if (tag.equals("ORGANIC")) {
                loadOrganic();
            } else if (tag.equals("ALL")) {
                loadAllProducts();
            } else {
                loadByCategory(tag);
            }
        });

        loadAllProducts();
    }

    /**
     * Wires the hamburger icon, edge-swipe, and every sidebar nav row to the
     * DrawerLayout added around the existing customer-home content. This is
     * the only new navigation mechanism introduced; every destination below
     * reuses an existing (or, for Profile/Address/Reviews/Settings, a new
     * minimal) Activity rather than duplicating any screen.
     */
    private void setupDrawer(Toolbar toolbar) {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        TextView drawerName = findViewById(R.id.drawerCustomerName);
        drawerName.setText(sessionManager.getName());

        findViewById(R.id.drawerProfileRow).setOnClickListener(v -> openDrawerDestination(CustomerProfileActivity.class));

        findViewById(R.id.navHome).setOnClickListener(v -> drawerLayout.closeDrawer(Gravity.START));
        findViewById(R.id.navCategories).setOnClickListener(v -> openDrawerDestination(CategoriesActivity.class));
        findViewById(R.id.navCart).setOnClickListener(v -> openDrawerDestination(CartActivity.class));
        findViewById(R.id.navOrders).setOnClickListener(v -> openDrawerDestination(OrderHistoryActivity.class));
        findViewById(R.id.navReviews).setOnClickListener(v -> openDrawerDestination(CustomerReviewsActivity.class));
        findViewById(R.id.navProfile).setOnClickListener(v -> openDrawerDestination(CustomerProfileActivity.class));
        findViewById(R.id.navAddress).setOnClickListener(v -> openDrawerDestination(CustomerAddressActivity.class));
        findViewById(R.id.navSettings).setOnClickListener(v -> openDrawerDestination(CustomerSettingsActivity.class));
        findViewById(R.id.navLogout).setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            sessionManager.logout();
            startActivity(new Intent(this, com.farmconnect.android.ui.auth.LoginActivity.class));
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
        getMenuInflater().inflate(R.menu.menu_customer_home, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });

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

    /**
     * Backend is the source of truth for the badge (GET
     * /api/notifications/unread-count) - refreshed every time Home comes
     * back into view (app reopen, returning from Notifications after
     * marking things read, after a new order notification arrives, etc.)
     */
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_cart) {
            startActivity(new Intent(this, CartActivity.class));
            return true;
        } else if (id == R.id.action_notifications) {
            startActivity(new Intent(this, com.farmconnect.android.ui.common.NotificationsActivity.class));
            return true;
        } else if (id == R.id.action_orders) {
            startActivity(new Intent(this, OrderHistoryActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            sessionManager.logout();
            startActivity(new Intent(this, com.farmconnect.android.ui.auth.LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUnreadNotificationCount();
    }
    private void loadAllProducts() {
        swipeRefresh.setRefreshing(true);
        apiService.getAllProducts().enqueue(listCallback());
    }

    private void loadByCategory(String category) {
        swipeRefresh.setRefreshing(true);
        apiService.getByCategory(category).enqueue(listCallback());
    }

    private void loadOrganic() {
        swipeRefresh.setRefreshing(true);
        apiService.getOrganic().enqueue(listCallback());
    }

    private void search(String query) {
        swipeRefresh.setRefreshing(true);
        apiService.search(query).enqueue(listCallback());
    }

    private Callback<List<Product>> listCallback() {
        return new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateData(response.body());
                } else {
                    Toast.makeText(CustomerHomeActivity.this, "Could not load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CustomerHomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
    }
}
