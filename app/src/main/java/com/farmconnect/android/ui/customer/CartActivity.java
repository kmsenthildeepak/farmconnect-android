package com.farmconnect.android.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.CartAdapter;
import com.farmconnect.android.model.CartModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity
        implements CartAdapter.Listener {

    private RecyclerView cartList;

    private TextView totalText;

    private TextView emptyText;

    private Button checkoutButton;

    private Button clearCartButton;

    private CartAdapter adapter;

    private ApiService apiService;

    private final List<CartModels.CartItem> items =
            new ArrayList<>();

    /*
     * Prevent multiple cart operations from being triggered
     * at the same time.
     */
    private boolean operationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cart);

        // =========================================================
        // TOOLBAR
        // =========================================================

        Toolbar toolbar =
                findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(
                v -> finish()
        );

        // =========================================================
        // VIEWS
        // =========================================================

        cartList =
                findViewById(R.id.cartList);

        totalText =
                findViewById(R.id.totalText);

        emptyText =
                findViewById(R.id.emptyText);

        checkoutButton =
                findViewById(R.id.checkoutButton);

        clearCartButton =
                findViewById(R.id.clearCartButton);

        // =========================================================
        // API
        // =========================================================

        apiService =
                ApiClient.getApiService(this);

        // =========================================================
        // RECYCLER VIEW
        // =========================================================

        cartList.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new CartAdapter(
                        this,
                        items,
                        this
                );

        cartList.setAdapter(adapter);

        // =========================================================
        // CHECKOUT
        // =========================================================

        checkoutButton.setOnClickListener(
                v -> {

                    if (operationInProgress) {
                        return;
                    }

                    if (items.isEmpty()) {

                        Toast.makeText(
                                CartActivity.this,
                                "Your cart is empty",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    Intent intent =
                            new Intent(
                                    CartActivity.this,
                                    CheckoutActivity.class
                            );

                    startActivity(intent);
                }
        );

        // =========================================================
        // CLEAR CART
        // =========================================================

        clearCartButton.setOnClickListener(
                v -> {

                    if (operationInProgress) {
                        return;
                    }

                    if (items.isEmpty()) {

                        Toast.makeText(
                                CartActivity.this,
                                "Your cart is already empty",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    showClearCartConfirmation();
                }
        );

        // =========================================================
        // INITIAL STATE
        // =========================================================

        showLoadingState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadCart();
    }

    // =============================================================
    // LOAD CART
    // =============================================================

    private void loadCart() {

        if (operationInProgress) {
            return;
        }

        showLoadingState();

        apiService
                .getCart()
                .enqueue(
                        new Callback<CartModels.CartResponse>() {

                            @Override
                            public void onResponse(
                                    Call<CartModels.CartResponse> call,
                                    Response<CartModels.CartResponse> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    bind(
                                            response.body()
                                    );

                                } else {

                                    handleApiError(
                                            response,
                                            "Could not load your cart."
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<CartModels.CartResponse> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                showEmptyState(
                                        "Unable to load cart.\nPlease check your connection and try again."
                                );

                                Toast.makeText(
                                        CartActivity.this,
                                        "Network error: "
                                                + safeMessage(t),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // BIND CART
    // =============================================================

    private void bind(
            CartModels.CartResponse cart
    ) {

        if (cart == null) {
            showEmptyState("Your cart is empty");
            return;
        }

        items.clear();

        if (cart.items != null) {
            items.addAll(cart.items);
        }

        adapter.notifyDataSetChanged();

        boolean empty =
                items.isEmpty();

        if (empty) {

            showEmptyState(
                    "Your cart is empty"
            );

            totalText.setText(
                    "Total: \u20B90.00"
            );

            clearCartButton.setEnabled(false);

            checkoutButton.setEnabled(false);

            return;
        }

        // =========================================================
        // SHOW CART
        // =========================================================

        emptyText.setVisibility(
                View.GONE
        );

        cartList.setVisibility(
                View.VISIBLE
        );

        checkoutButton.setEnabled(
                !operationInProgress
        );

        clearCartButton.setEnabled(
                !operationInProgress
        );

        // =========================================================
        // TOTAL
        // =========================================================

        double total =
                cart.total;

        totalText.setText(
                String.format(
                        Locale.getDefault(),
                        "Total: \u20B9%.2f",
                        total
                )
        );
    }

    // =============================================================
    // EMPTY STATE
    // =============================================================

    private void showEmptyState(
            String message
    ) {

        emptyText.setText(
                message
        );

        emptyText.setVisibility(
                View.VISIBLE
        );

        cartList.setVisibility(
                View.GONE
        );

        checkoutButton.setEnabled(
                false
        );

        clearCartButton.setEnabled(
                false
        );
    }

    // =============================================================
    // LOADING STATE
    // =============================================================

    private void showLoadingState() {

        emptyText.setText(
                "Loading your cart..."
        );

        emptyText.setVisibility(
                View.VISIBLE
        );

        cartList.setVisibility(
                View.GONE
        );

        checkoutButton.setEnabled(
                false
        );

        clearCartButton.setEnabled(
                false
        );

        totalText.setText(
                "Total: \u20B90.00"
        );
    }

    // =============================================================
    // OPERATION STATE
    // =============================================================

    private void setOperationState(
            boolean inProgress
    ) {

        operationInProgress =
                inProgress;

        if (inProgress) {

            checkoutButton.setEnabled(false);

            clearCartButton.setEnabled(false);

        } else {

            boolean hasItems =
                    !items.isEmpty();

            checkoutButton.setEnabled(
                    hasItems
            );

            clearCartButton.setEnabled(
                    hasItems
            );
        }

        adapter.notifyDataSetChanged();
    }

    // =============================================================
    // INCREASE
    // =============================================================

    @Override
    public void onIncrease(
            CartModels.CartItem item
    ) {

        if (item == null
                || operationInProgress) {
            return;
        }

        int requestedQuantity =
                item.quantity + 1;

        /*
         * Optional client-side validation.
         *
         * The backend remains the final authority because
         * stock can change after the cart was loaded.
         */
        if (item.availableStock > 0
                && requestedQuantity > item.availableStock) {

            Toast.makeText(
                    this,
                    "Only "
                            + item.availableStock
                            + " available in stock",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        updateQuantity(
                item.cartItemId,
                requestedQuantity
        );
    }

    // =============================================================
    // DECREASE
    // =============================================================

    @Override
    public void onDecrease(
            CartModels.CartItem item
    ) {

        if (item == null
                || operationInProgress) {
            return;
        }

        if (item.quantity <= 1) {

            Toast.makeText(
                    this,
                    "Quantity cannot be less than 1",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        updateQuantity(
                item.cartItemId,
                item.quantity - 1
        );
    }

    // =============================================================
    // UPDATE QUANTITY
    // =============================================================

    private void updateQuantity(
            long cartItemId,
            int newQty
    ) {

        if (operationInProgress) {
            return;
        }

        if (newQty < 1) {
            newQty = 1;
        }

        final int requestedQuantity =
                newQty;

        setOperationState(true);

        apiService
                .updateCartItem(
                        cartItemId,
                        requestedQuantity
                )
                .enqueue(
                        new Callback<CartModels.CartResponse>() {

                            @Override
                            public void onResponse(
                                    Call<CartModels.CartResponse> call,
                                    Response<CartModels.CartResponse> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    bind(
                                            response.body()
                                    );

                                    setOperationState(false);

                                } else {

                                    setOperationState(false);

                                    String message =
                                            getErrorMessage(
                                                    response,
                                                    "Could not update quantity."
                                            );

                                    Toast.makeText(
                                            CartActivity.this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<CartModels.CartResponse> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                setOperationState(false);

                                Toast.makeText(
                                        CartActivity.this,
                                        "Network error: "
                                                + safeMessage(t),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // REMOVE ITEM
    // =============================================================

    @Override
    public void onRemove(
            CartModels.CartItem item
    ) {

        if (item == null
                || operationInProgress) {
            return;
        }

        showRemoveConfirmation(item);
    }

    // =============================================================
    // REMOVE CONFIRMATION
    // =============================================================

    private void showRemoveConfirmation(
            CartModels.CartItem item
    ) {

        String productName =
                item.productName == null
                        || item.productName.trim().isEmpty()
                        ? "this product"
                        : item.productName.trim();

        new AlertDialog.Builder(this)
                .setTitle("Remove item")
                .setMessage(
                        "Remove \""
                                + productName
                                + "\" from your cart?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Remove",
                        (dialog, which) ->
                                removeItem(item)
                )
                .show();
    }

    // =============================================================
    // REMOVE ITEM API
    // =============================================================

    private void removeItem(
            CartModels.CartItem item
    ) {

        if (item == null
                || operationInProgress) {
            return;
        }

        setOperationState(true);

        apiService
                .removeCartItem(
                        item.cartItemId
                )
                .enqueue(
                        new Callback<CartModels.CartResponse>() {

                            @Override
                            public void onResponse(
                                    Call<CartModels.CartResponse> call,
                                    Response<CartModels.CartResponse> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    /*
                                     * Replace the entire local cart
                                     * with the server response.
                                     */
                                    bind(
                                            response.body()
                                    );

                                    setOperationState(false);

                                    Toast.makeText(
                                            CartActivity.this,
                                            "Product removed from cart",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    /*
                                     * If the backend response somehow
                                     * still contains the removed item,
                                     * reload the cart once to ensure the
                                     * UI reflects the database state.
                                     */
                                    loadCartAfterMutation();

                                } else {

                                    setOperationState(false);

                                    Toast.makeText(
                                            CartActivity.this,
                                            getErrorMessage(
                                                    response,
                                                    "Could not remove product from cart."
                                            ),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<CartModels.CartResponse> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                setOperationState(false);

                                Toast.makeText(
                                        CartActivity.this,
                                        "Network error: "
                                                + safeMessage(t),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // CLEAR CART CONFIRMATION
    // =============================================================

    private void showClearCartConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Clear cart")
                .setMessage(
                        "Are you sure you want to remove all products from your cart?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Clear Cart",
                        (dialog, which) ->
                                clearCart()
                )
                .show();
    }

    // =============================================================
    // CLEAR CART API
    // =============================================================

    private void clearCart() {

        if (operationInProgress) {
            return;
        }

        setOperationState(true);

        apiService
                .clearCart()
                .enqueue(
                        new Callback<Void>() {

                            @Override
                            public void onResponse(
                                    Call<Void> call,
                                    Response<Void> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                if (response.isSuccessful()) {

                                    /*
                                     * Backend returns 204 No Content.
                                     *
                                     * Therefore we do not expect a
                                     * CartResponse here.
                                     */

                                    items.clear();

                                    adapter.notifyDataSetChanged();

                                    totalText.setText(
                                            "Total: \u20B90.00"
                                    );

                                    setOperationState(false);

                                    showEmptyState(
                                            "Your cart is empty"
                                    );

                                    Toast.makeText(
                                            CartActivity.this,
                                            "Cart cleared",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } else {

                                    setOperationState(false);

                                    Toast.makeText(
                                            CartActivity.this,
                                            getErrorMessage(
                                                    response,
                                                    "Could not clear cart."
                                            ),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<Void> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                setOperationState(false);

                                Toast.makeText(
                                        CartActivity.this,
                                        "Network error: "
                                                + safeMessage(t),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // RELOAD AFTER MUTATION
    // =============================================================

    private void loadCartAfterMutation() {

        if (operationInProgress) {
            return;
        }

        apiService
                .getCart()
                .enqueue(
                        new Callback<CartModels.CartResponse>() {

                            @Override
                            public void onResponse(
                                    Call<CartModels.CartResponse> call,
                                    Response<CartModels.CartResponse> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    bind(
                                            response.body()
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<CartModels.CartResponse> call,
                                    Throwable t
                            ) {
                                /*
                                 * The mutation itself already succeeded.
                                 * Do not show a second error toast here.
                                 */
                            }
                        }
                );
    }

    // =============================================================
    // API ERROR HANDLING
    // =============================================================

    private void handleApiError(
            Response<?> response,
            String defaultMessage
    ) {

        showEmptyState(
                defaultMessage
        );

        Toast.makeText(
                this,
                getErrorMessage(
                        response,
                        defaultMessage
                ),
                Toast.LENGTH_LONG
        ).show();
    }

    // =============================================================
    // ERROR MESSAGE
    // =============================================================

    private String getErrorMessage(
            Response<?> response,
            String defaultMessage
    ) {

        if (response == null) {
            return defaultMessage;
        }

        /*
         * First try to read the backend's JSON ErrorResponse.
         *
         * Backend format:
         *
         * {
         *   "timestamp": "...",
         *   "status": 400,
         *   "error": "Bad Request",
         *   "message": "Only 5 kg available in stock",
         *   "path": "/api/customer/cart/items/..."
         * }
         */

        try {

            if (response.errorBody() != null) {

                String body =
                        response.errorBody()
                                .string();

                if (body != null
                        && !body.trim().isEmpty()) {

                    JSONObject json =
                            new JSONObject(body);

                    if (json.has("message")
                            && !json.isNull("message")) {

                        String backendMessage =
                                json.getString("message");

                        if (backendMessage != null
                                && !backendMessage.trim().isEmpty()) {

                            return backendMessage.trim();
                        }
                    }
                }
            }

        } catch (Exception ignored) {
            /*
             * Fall back to status-code handling below.
             */
        }

        // =========================================================
        // STATUS CODE FALLBACK
        // =========================================================

        switch (response.code()) {

            case 400:
                return defaultMessage
                        + " (HTTP 400)";

            case 401:
                return "Your session has expired. Please login again.";

            case 403:
                return "You are not allowed to access the cart.";

            case 404:
                return "Cart service or cart item was not found.";

            case 409:
                return "The cart could not be updated because stock is unavailable.";

            case 500:
                return "Server error. Please try again.";

            default:
                return defaultMessage
                        + " (HTTP "
                        + response.code()
                        + ")";
        }
    }

    // =============================================================
    // SAFE NETWORK MESSAGE
    // =============================================================

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