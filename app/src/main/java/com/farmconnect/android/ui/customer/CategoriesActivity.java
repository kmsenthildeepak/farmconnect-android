package com.farmconnect.android.ui.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.ProductAdapter;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reached from the sidebar's "Categories" item.
 *
 * Distinct from Home: Home shows a mixed/recent feed of all products with
 * quick category chips; this screen is a dedicated category browser (one
 * category selected at a time, via the existing
 * GET /api/products/category/{category} endpoint - no new backend work
 * needed, no duplicated filtering logic).
 */
public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView productGrid;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyState;
    private ChipGroup categoryTiles;
    private ProductAdapter adapter;
    private final List<Product> products = new ArrayList<>();
    private ApiService apiService;
    private String currentCategory = "VEGETABLES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        productGrid = findViewById(R.id.categoriesProductGrid);
        swipeRefresh = findViewById(R.id.categoriesSwipeRefresh);
        emptyState = findViewById(R.id.categoriesEmptyState);
        categoryTiles = findViewById(R.id.categoryTiles);

        apiService = ApiClient.getApiService(this);

        productGrid.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(this, products);
        productGrid.setAdapter(adapter);

        categoryTiles.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = findViewById(checkedIds.get(0));
            currentCategory = String.valueOf(chip.getTag());
            loadCategory();
        });

        swipeRefresh.setOnRefreshListener(this::loadCategory);

        loadCategory();
    }

    private void loadCategory() {
        swipeRefresh.setRefreshing(true);
        apiService.getByCategory(currentCategory).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CategoriesActivity.this,
                            "Could not load products", Toast.LENGTH_SHORT).show();
                    return;
                }
                products.clear();
                products.addAll(response.body());
                adapter.notifyDataSetChanged();

                boolean empty = products.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                productGrid.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CategoriesActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
