package com.farmconnect.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.farmconnect.android.R;
import com.farmconnect.android.model.AuthModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    private EditText addressInput;
    private EditText cityInput;
    private EditText stateInput;
    private EditText pincodeInput;

    private EditText farmNameInput;
    private EditText farmAddressInput;

    private Spinner roleSpinner;

    private LinearLayout farmerFieldsContainer;

    private Button registerButton;

    private TextView loginLink;

    private ProgressBar progressBar;

    private ApiService apiService;

    /*
     * Public registration roles.
     *
     * CUSTOMER:
     * Can register normally and login after registration.
     *
     * FARMER:
     * Can register normally but must be verified by admin.
     *
     * ADMIN:
     * Is intentionally NOT available for public registration.
     */
    private static final String[] REGISTRATION_ROLES = {
            "CUSTOMER",
            "FARMER"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // ---------------------------------------------------------
        // Find views
        // ---------------------------------------------------------

        nameInput = findViewById(R.id.nameInput);

        emailInput = findViewById(R.id.emailInput);

        phoneInput = findViewById(R.id.phoneInput);

        passwordInput = findViewById(R.id.passwordInput);

        confirmPasswordInput =
                findViewById(R.id.confirmPasswordInput);

        com.farmconnect.android.util.PasswordToggleHelper.attach(passwordInput);
        com.farmconnect.android.util.PasswordToggleHelper.attach(confirmPasswordInput);

        addressInput =
                findViewById(R.id.addressInput);

        cityInput =
                findViewById(R.id.cityInput);

        stateInput =
                findViewById(R.id.stateInput);

        pincodeInput =
                findViewById(R.id.pincodeInput);

        farmNameInput =
                findViewById(R.id.farmNameInput);

        farmAddressInput =
                findViewById(R.id.farmAddressInput);

        roleSpinner =
                findViewById(R.id.roleSpinner);

        farmerFieldsContainer =
                findViewById(R.id.farmerFieldsContainer);

        registerButton =
                findViewById(R.id.registerButton);

        loginLink =
                findViewById(R.id.loginLink);

        progressBar =
                findViewById(R.id.progressBar);

        // ---------------------------------------------------------
        // API
        // ---------------------------------------------------------

        apiService =
                ApiClient.getApiService(this);

        // ---------------------------------------------------------
        // Role dropdown
        // ---------------------------------------------------------

        ArrayAdapter<String> roleAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        REGISTRATION_ROLES
                );

        roleAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        roleSpinner.setAdapter(roleAdapter);

        // ---------------------------------------------------------
        // Role selection
        // ---------------------------------------------------------

        roleSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        String selectedRole =
                                String.valueOf(
                                        roleSpinner.getSelectedItem()
                                );

                        boolean isFarmer =
                                "FARMER".equalsIgnoreCase(
                                        selectedRole
                                );

                        farmerFieldsContainer.setVisibility(
                                isFarmer
                                        ? View.VISIBLE
                                        : View.GONE
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {

                        farmerFieldsContainer.setVisibility(
                                View.GONE
                        );
                    }
                }
        );

        // ---------------------------------------------------------
        // Register button
        // ---------------------------------------------------------

        registerButton.setOnClickListener(
                v -> attemptRegister()
        );

        // ---------------------------------------------------------
        // Already have an account?
        // ---------------------------------------------------------

        loginLink.setOnClickListener(
                v -> finish()
        );
    }

    // =============================================================
    // REGISTER
    // =============================================================

    private void attemptRegister() {

        String name =
                nameInput.getText()
                        .toString()
                        .trim();

        String email =
                emailInput.getText()
                        .toString()
                        .trim();

        String phone =
                phoneInput.getText()
                        .toString()
                        .trim();

        String password =
                passwordInput.getText()
                        .toString()
                        .trim();

        String confirmPassword =
                confirmPasswordInput.getText()
                        .toString()
                        .trim();

        // ---------------------------------------------------------
        // Required field validation
        // ---------------------------------------------------------

        if (TextUtils.isEmpty(name)
                || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(phone)
                || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(confirmPassword)) {

            Toast.makeText(
                    this,
                    "Please fill all required fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ---------------------------------------------------------
        // Email validation
        // ---------------------------------------------------------

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            Toast.makeText(
                    this,
                    "Please enter a valid email address",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ---------------------------------------------------------
        // Password validation
        // ---------------------------------------------------------

        if (!password.equals(confirmPassword)) {

            Toast.makeText(
                    this,
                    "Passwords do not match",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ---------------------------------------------------------
        // Selected role
        // ---------------------------------------------------------

        String selectedRole =
                String.valueOf(
                        roleSpinner.getSelectedItem()
                );

        boolean isFarmer =
                "FARMER".equalsIgnoreCase(
                        selectedRole
                );

        // ---------------------------------------------------------
        // Farmer information
        // ---------------------------------------------------------

        String farmName =
                farmNameInput.getText()
                        .toString()
                        .trim();

        String farmAddress =
                farmAddressInput.getText()
                        .toString()
                        .trim();

        if (isFarmer
                && TextUtils.isEmpty(farmName)) {

            Toast.makeText(
                    this,
                    "Please enter your farm name",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ---------------------------------------------------------
        // Build registration request
        // ---------------------------------------------------------

        AuthModels.RegisterRequest req =
                new AuthModels.RegisterRequest();

        req.name =
                name;

        req.email =
                email;

        req.phone =
                phone;

        req.password =
                password;

        req.confirmPassword =
                confirmPassword;

        req.role =
                isFarmer
                        ? "FARMER"
                        : "CUSTOMER";

        req.address =
                addressInput.getText()
                        .toString()
                        .trim();

        req.city =
                cityInput.getText()
                        .toString()
                        .trim();

        req.state =
                stateInput.getText()
                        .toString()
                        .trim();

        req.pincode =
                pincodeInput.getText()
                        .toString()
                        .trim();

        if (isFarmer) {

            req.farmName =
                    farmName;

            req.farmAddress =
                    farmAddress;
        }

        // ---------------------------------------------------------
        // Send registration request
        // ---------------------------------------------------------

        setLoading(true);

        apiService.register(req)
                .enqueue(
                        new Callback<AuthModels.AuthResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AuthModels.AuthResponse> call,
                                    Response<AuthModels.AuthResponse> response) {

                                setLoading(false);

                                // -------------------------------------------------
                                // SUCCESS
                                // -------------------------------------------------

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    /*
                                     * VERY IMPORTANT:
                                     *
                                     * DO NOT save the JWT here.
                                     *
                                     * Registration only creates the account.
                                     *
                                     * The user must manually login afterward.
                                     *
                                     * Therefore there is intentionally NO:
                                     *
                                     * sessionManager.saveSession(...)
                                     *
                                     * here.
                                     */

                                    String registeredEmail =
                                            response.body().email;

                                    if (registeredEmail == null
                                            || registeredEmail.trim().isEmpty()) {

                                        registeredEmail =
                                                email;
                                    }

                                    /*
                                     * Show successful registration message.
                                     */

                                    Toast.makeText(
                                            RegisterActivity.this,
                                            "Registration successful! Please login with your registered email and password.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    /*
                                     * Open LoginActivity.
                                     *
                                     * Pass the registered email so the
                                     * login screen can automatically fill
                                     * the email field.
                                     */

                                    Intent intent =
                                            new Intent(
                                                    RegisterActivity.this,
                                                    LoginActivity.class
                                            );

                                    intent.putExtra(
                                            "registeredEmail",
                                            registeredEmail
                                    );

                                    /*
                                     * Prevent duplicate LoginActivity
                                     * instances.
                                     */

                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    );

                                    startActivity(intent);

                                    /*
                                     * Remove RegisterActivity from
                                     * the back stack.
                                     */

                                    finish();

                                    return;
                                }

                                // -------------------------------------------------
                                // REGISTRATION FAILED
                                // -------------------------------------------------

                                String message;

                                if (response.code() == 400) {

                                    message =
                                            "Registration failed. Please check your details or the email may already be registered.";

                                } else if (response.code() == 409) {

                                    message =
                                            "An account with this email already exists.";

                                } else {

                                    message =
                                            "Registration failed. Please try again.";
                                }

                                Toast.makeText(
                                        RegisterActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                            // -----------------------------------------------------
                            // NETWORK ERROR
                            // -----------------------------------------------------

                            @Override
                            public void onFailure(
                                    Call<AuthModels.AuthResponse> call,
                                    Throwable t) {

                                setLoading(false);

                                String errorMessage =
                                        t.getMessage();

                                if (errorMessage == null
                                        || errorMessage.trim().isEmpty()) {

                                    errorMessage =
                                            "Unable to connect to the server.";
                                }

                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Network error: "
                                                + errorMessage,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =============================================================
    // LOADING STATE
    // =============================================================

    private void setLoading(boolean loading) {

        progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        registerButton.setEnabled(
                !loading
        );

        roleSpinner.setEnabled(
                !loading
        );
    }
}