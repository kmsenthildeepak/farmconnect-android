package com.farmconnect.android.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.farmconnect.android.R;
import com.farmconnect.android.model.OrderModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    private EditText addressInput;
    private EditText contactPhoneInput;

    private RadioGroup paymentGroup;

    private Button placeOrderButton;

    private ProgressBar progressBar;

    private ApiService apiService;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_checkout
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

        addressInput =
                findViewById(
                        R.id.shippingAddressInput
                );

        contactPhoneInput =
                findViewById(
                        R.id.contactPhoneInput
                );

        paymentGroup =
                findViewById(
                        R.id.paymentGroup
                );

        placeOrderButton =
                findViewById(
                        R.id.placeOrderButton
                );

        progressBar =
                findViewById(
                        R.id.progressBar
                );

        // ---------------------------------------------------------
        // API
        // ---------------------------------------------------------

        apiService =
                ApiClient.getApiService(
                        this
                );

        prefillSavedAddress();

        // ---------------------------------------------------------
        // Place order
        // ---------------------------------------------------------

        placeOrderButton.setOnClickListener(
                v -> placeOrder()
        );
    }

    /**
     * Prefills the shipping address field from the customer's saved
     * address (see CustomerAddressActivity / GET /api/users/me) so they
     * don't have to retype it every time. Purely a convenience - the
     * field stays editable and the order still sends whatever text is in
     * it at place-order time.
     */
    private void prefillSavedAddress() {
        apiService.getMyProfile().enqueue(new Callback<com.farmconnect.android.model.UserProfile>() {
            @Override
            public void onResponse(Call<com.farmconnect.android.model.UserProfile> call,
                                    Response<com.farmconnect.android.model.UserProfile> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                if (!TextUtils.isEmpty(addressInput.getText())) {
                    return; // don't overwrite anything the user already typed
                }
                com.farmconnect.android.model.UserProfile profile = response.body();
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(profile.address)) sb.append(profile.address);
                if (!TextUtils.isEmpty(profile.city)) sb.append(", ").append(profile.city);
                if (!TextUtils.isEmpty(profile.state)) sb.append(", ").append(profile.state);
                if (!TextUtils.isEmpty(profile.pincode)) sb.append(" - ").append(profile.pincode);
                if (sb.length() > 0) {
                    addressInput.setText(sb.toString());
                }
                if (TextUtils.isEmpty(contactPhoneInput.getText()) && !TextUtils.isEmpty(profile.phone)) {
                    contactPhoneInput.setText(profile.phone);
                }
            }

            @Override
            public void onFailure(Call<com.farmconnect.android.model.UserProfile> call, Throwable t) {
                // Silent - this is just a convenience prefill, not required for checkout.
            }
        });
    }

    // =============================================================
    // PLACE ORDER
    // =============================================================

    private void placeOrder() {

        String address =
                addressInput
                        .getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(address)) {

            addressInput.setError(
                    "Shipping address is required"
            );

            addressInput.requestFocus();

            Toast.makeText(
                    this,
                    "Please enter a shipping address",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ---------------------------------------------------------
        // Contact mobile number (mandatory, 10 digits)
        // ---------------------------------------------------------

        String contactPhone =
                contactPhoneInput
                        .getText()
                        .toString()
                        .trim();

        if (!contactPhone.matches("^[6-9]\\d{9}$")) {

            contactPhoneInput.setError(
                    "Enter a valid 10-digit mobile number"
            );

            contactPhoneInput.requestFocus();

            Toast.makeText(
                    this,
                    "Please enter a valid 10-digit mobile number",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ---------------------------------------------------------
        // Payment method
        // ---------------------------------------------------------

        String paymentMethod;

        int checkedId =
                paymentGroup
                        .getCheckedRadioButtonId();

        if (checkedId == R.id.paymentUpi) {

            paymentMethod = "UPI";

        } else if (checkedId == R.id.paymentCard) {

            paymentMethod = "CARD";

        } else {

            paymentMethod = "COD";
        }

        // ---------------------------------------------------------
        // Request
        // ---------------------------------------------------------

        OrderModels.PlaceOrderRequest req =
                new OrderModels.PlaceOrderRequest();

        req.shippingAddress =
                address;

        req.contactPhone =
                contactPhone;

        req.paymentMethod =
                paymentMethod;

        // ---------------------------------------------------------
        // Loading
        // ---------------------------------------------------------

        setLoading(
                true
        );

        // ---------------------------------------------------------
        // API
        // ---------------------------------------------------------

        apiService
                .placeOrder(req)
                .enqueue(
                        new Callback<OrderModels.OrderResponse>() {

                            @Override
                            public void onResponse(
                                    Call<OrderModels.OrderResponse> call,
                                    Response<OrderModels.OrderResponse> response
                            ) {

                                setLoading(
                                        false
                                );

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    OrderModels.OrderResponse order =
                                            response.body();

                                    Toast.makeText(
                                            CheckoutActivity.this,
                                            "Order placed successfully!",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    /*
                                     * Do NOT create another order.
                                     *
                                     * The backend has already created
                                     * the order.
                                     *
                                     * Open order history so the user
                                     * can verify it.
                                     */

                                    Intent intent =
                                            new Intent(
                                                    CheckoutActivity.this,
                                                    OrderHistoryActivity.class
                                            );

                                    /*
                                     * Remove checkout from the
                                     * current flow.
                                     */

                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    );

                                    startActivity(
                                            intent
                                    );

                                    finish();

                                } else {

                                    String message =
                                            getOrderErrorMessage(
                                                    response
                                            );

                                    Toast.makeText(
                                            CheckoutActivity.this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<OrderModels.OrderResponse> call,
                                    Throwable t
                            ) {

                                setLoading(
                                        false
                                );

                                Toast.makeText(
                                        CheckoutActivity.this,
                                        "Network error: "
                                                + safeMessage(t),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // LOADING
    // =============================================================

    private void setLoading(
            boolean loading
    ) {

        if (progressBar != null) {

            progressBar.setVisibility(
                    loading
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (placeOrderButton != null) {

            placeOrderButton.setEnabled(
                    !loading
            );
        }

        if (addressInput != null) {

            addressInput.setEnabled(
                    !loading
            );
        }

        if (paymentGroup != null) {

            paymentGroup.setEnabled(
                    !loading
            );
        }
    }

    // =============================================================
    // ERROR
    // =============================================================

    private String getOrderErrorMessage(
            Response<?> response
    ) {

        if (response == null) {

            return "Could not place order.";
        }

        switch (response.code()) {

            case 400:
                return "Could not place order. Please check your details.";

            case 401:
                return "Your session has expired. Please login again.";

            case 403:
                return "You are not allowed to place an order.";

            case 409:
                return "Could not place order. Product stock may have changed.";

            case 500:
                return "Server error while placing the order.";

            default:
                return "Could not place order. HTTP "
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