package com.farmconnect.android.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.FarmerProductAdapter;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageProductsActivity extends AppCompatActivity
        implements FarmerProductAdapter.Listener {

    private RecyclerView productList;
    private TextView emptyText;
    private FarmerProductAdapter adapter;
    private ApiService apiService;
    private final List<Product> products = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_products);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        productList = findViewById(R.id.farmerProductList);
        emptyText = findViewById(R.id.emptyProductsText);

        FloatingActionButton fab = findViewById(R.id.addProductFab);

        apiService = ApiClient.getApiService(this);

        productList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FarmerProductAdapter(
                this,
                products,
                this
        );

        productList.setAdapter(adapter);

        fab.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                ManageProductsActivity.this,
                                AddEditProductActivity.class
                        )
                )
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {

        apiService.myProducts().enqueue(new Callback<List<Product>>() {

            @Override
            public void onResponse(
                    Call<List<Product>> call,
                    Response<List<Product>> response
            ) {

                if (response.isSuccessful() && response.body() != null) {

                    products.clear();
                    products.addAll(response.body());

                    adapter.notifyDataSetChanged();

                    updateEmptyState();

                } else {

                    String error = getErrorMessage(response);

                    Toast.makeText(
                            ManageProductsActivity.this,
                            "Could not load products. HTTP "
                                    + response.code()
                                    + (error.isEmpty() ? "" : ": " + error),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<List<Product>> call,
                    Throwable t
            ) {

                Toast.makeText(
                        ManageProductsActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateEmptyState() {

        if (products.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            productList.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            productList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEdit(Product product) {

        Intent intent = new Intent(
                this,
                AddEditProductActivity.class
        );

        intent.putExtra(
                "productId",
                product.productId
        );

        startActivity(intent);
    }

    @Override
    public void onDelete(Product product) {

        Log.d("DELETE_TEST", "Opening delete dialog for: " + product.productName);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete product")
                .setMessage(
                        "Delete \"" + product.productName +
                                "\"? This cannot be undone."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {

            Button cancelButton =
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            Button deleteButton =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            // ---------------------------------------------------------
            // CANCEL
            // ---------------------------------------------------------

            cancelButton.setOnClickListener(v -> {

                Log.d(
                        "DELETE_TEST",
                        "CANCEL clicked for: " + product.productName
                );

                Toast.makeText(
                        ManageProductsActivity.this,
                        "Delete cancelled",
                        Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();
            });

            // ---------------------------------------------------------
            // DELETE
            // ---------------------------------------------------------

            deleteButton.setOnClickListener(v -> {

                Log.d(
                        "DELETE_TEST",
                        "DELETE clicked for product ID: "
                                + product.productId
                );

                deleteButton.setEnabled(false);
                cancelButton.setEnabled(false);

                Toast.makeText(
                        ManageProductsActivity.this,
                        "Deleting " + product.productName + "...",
                        Toast.LENGTH_SHORT
                ).show();

                apiService.deleteProduct(product.productId)
                        .enqueue(new Callback<Void>() {

                            @Override
                            public void onResponse(
                                    Call<Void> call,
                                    Response<Void> response
                            ) {

                                Log.d(
                                        "DELETE_TEST",
                                        "Delete response code: "
                                                + response.code()
                                );

                                if (response.isSuccessful()) {

                                    Log.d(
                                            "DELETE_TEST",
                                            "DELETE SUCCESS: "
                                                    + product.productName
                                    );

                                    dialog.dismiss();

                                    Toast.makeText(
                                            ManageProductsActivity.this,
                                            "Product deleted successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    loadProducts();

                                } else {

                                    Log.e(
                                            "DELETE_TEST",
                                            "DELETE FAILED. HTTP "
                                                    + response.code()
                                    );

                                    deleteButton.setEnabled(true);
                                    cancelButton.setEnabled(true);

                                    Toast.makeText(
                                            ManageProductsActivity.this,
                                            "Could not delete product. HTTP "
                                                    + response.code(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<Void> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        "DELETE_TEST",
                                        "DELETE NETWORK ERROR",
                                        t
                                );

                                deleteButton.setEnabled(true);
                                cancelButton.setEnabled(true);

                                Toast.makeText(
                                        ManageProductsActivity.this,
                                        "Network error: "
                                                + t.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
            });
        });

        dialog.show();
    }

    private void deleteProduct(Product product) {

        Toast.makeText(
                this,
                "Deleting product " + product.productId + "...",
                Toast.LENGTH_SHORT
        ).show();

        apiService.deleteProduct(
                product.productId
        ).enqueue(new Callback<Void>() {

            @Override
            public void onResponse(
                    Call<Void> call,
                    Response<Void> response
            ) {

                if (response.isSuccessful()) {

                    // Remove immediately from the local list.
                    int index = products.indexOf(product);

                    if (index >= 0) {
                        products.remove(index);
                        adapter.notifyItemRemoved(index);
                    } else {
                        // Fallback if object identity differs.
                        for (int i = 0; i < products.size(); i++) {
                            if (products.get(i).productId == product.productId) {
                                products.remove(i);
                                adapter.notifyItemRemoved(i);
                                break;
                            }
                        }
                    }

                    updateEmptyState();

                    Toast.makeText(
                            ManageProductsActivity.this,
                            "Product deleted successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    String error = getErrorMessage(response);

                    Toast.makeText(
                            ManageProductsActivity.this,
                            "Delete failed. HTTP "
                                    + response.code()
                                    + (error.isEmpty() ? "" : ": " + error),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<Void> call,
                    Throwable t
            ) {

                Toast.makeText(
                        ManageProductsActivity.this,
                        "Delete network error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private String getErrorMessage(Response<?> response) {

        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();

                if (body != null && !body.trim().isEmpty()) {
                    return body;
                }
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    @Override
    public void onToggleAvailability(Product product) {

        apiService.toggleAvailability(
                product.productId
        ).enqueue(new Callback<Product>() {

            @Override
            public void onResponse(
                    Call<Product> call,
                    Response<Product> response
            ) {

                if (!response.isSuccessful()) {

                    Toast.makeText(
                            ManageProductsActivity.this,
                            "Could not update availability. HTTP "
                                    + response.code(),
                            Toast.LENGTH_LONG
                    ).show();

                    loadProducts();
                }
            }

            @Override
            public void onFailure(
                    Call<Product> call,
                    Throwable t
            ) {

                Toast.makeText(
                        ManageProductsActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                loadProducts();
            }
        });
    }
}