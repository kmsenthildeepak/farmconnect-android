package com.farmconnect.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.farmconnect.android.R;
import com.farmconnect.android.model.AuthModels;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;
import com.farmconnect.android.ui.admin.AdminDashboardActivity;
import com.farmconnect.android.ui.customer.CustomerHomeActivity;
import com.farmconnect.android.ui.farmer.FarmerHomeActivity;
import com.farmconnect.android.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;

    private Button loginButton;

    private TextView registerLink;
    private TextView forgotPasswordLink;

    private ProgressBar progressBar;

    /*
     * Role selector.
     *
     * CUSTOMER
     * FARMER
     * ADMIN
     */
    private Spinner roleSpinner;

    private ApiService apiService;
    private SessionManager sessionManager;

    private static final String[] ROLES = {
            "CUSTOMER",
            "FARMER",
            "ADMIN"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        /*
         * ---------------------------------------------------------
         * FIND VIEWS
         * ---------------------------------------------------------
         */

        emailInput =
                findViewById(R.id.emailInput);

        passwordInput =
                findViewById(R.id.passwordInput);

        String registeredEmail =
                getIntent().getStringExtra("registeredEmail");

        if (registeredEmail != null
                && !registeredEmail.trim().isEmpty()) {

            emailInput.setText(registeredEmail);
            passwordInput.requestFocus();
        }

        checkBlockedNotice(getIntent());

        loginButton =
                findViewById(R.id.loginButton);

        registerLink =
                findViewById(R.id.registerLink);

        forgotPasswordLink =
                findViewById(R.id.forgotPasswordLink);

        progressBar =
                findViewById(R.id.progressBar);

        /*
         * ---------------------------------------------------------
         * ROLE SPINNER
         * ---------------------------------------------------------
         *
         * This expects activity_login.xml to contain:
         *
         * <Spinner
         *     android:id="@+id/roleSpinner"
         *     ... />
         *
         */

        roleSpinner =
                findViewById(R.id.roleSpinner);

        if (roleSpinner != null) {

            ArrayAdapter<String> roleAdapter =
                    new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            ROLES
                    );

            roleAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
            );

            roleSpinner.setAdapter(
                    roleAdapter
            );
        }

        /*
         * ---------------------------------------------------------
         * API + SESSION
         * ---------------------------------------------------------
         */

        apiService =
                ApiClient.getApiService(this);

        sessionManager =
                new SessionManager(this);

        /*
         * ---------------------------------------------------------
         * BUTTONS
         * ---------------------------------------------------------
         */

        loginButton.setOnClickListener(
                v -> attemptLogin()
        );

        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnClickListener(
                    v -> showForgotPasswordDialog()
            );
        }

        registerLink.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    LoginActivity.this,
                                    RegisterActivity.class
                            );

                    startActivity(intent);
                }
        );
    }

    /**
     * Attempt login.
     */
    private void attemptLogin() {

        if (isFinishing()
                || isDestroyed()) {

            return;
        }

        String email =
                emailInput
                        .getText()
                        .toString()
                        .trim();

        String password =
                passwordInput
                        .getText()
                        .toString()
                        .trim();

        /*
         * ---------------------------------------------------------
         * VALIDATION
         * ---------------------------------------------------------
         */

        if (TextUtils.isEmpty(email)) {

            emailInput.setError(
                    "Enter your email"
            );

            emailInput.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(password)) {

            passwordInput.setError(
                    "Enter your password"
            );

            passwordInput.requestFocus();

            return;
        }

        /*
         * Get selected role.
         */
        String selectedRole =
                getSelectedRole();

        if (TextUtils.isEmpty(selectedRole)) {

            Toast.makeText(
                    this,
                    "Please select your role",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * ---------------------------------------------------------
         * START LOGIN
         * ---------------------------------------------------------
         */

        setLoading(true);

        AuthModels.LoginRequest request =
                new AuthModels.LoginRequest(
                        email,
                        password
                );

        apiService
                .login(request)
                .enqueue(
                        new Callback<AuthModels.AuthResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AuthModels.AuthResponse> call,
                                    Response<AuthModels.AuthResponse> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }

                                setLoading(false);

                                /*
                                 * -------------------------------------------------
                                 * LOGIN SUCCESS
                                 * -------------------------------------------------
                                 */

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    AuthModels.AuthResponse body =
                                            response.body();

                                    String backendRole =
                                            normalizeRole(
                                                    body.role
                                            );

                                    /*
                                     * -------------------------------------------------
                                     * IMPORTANT ROLE CHECK
                                     * -------------------------------------------------
                                     *
                                     * The backend tells us what role this
                                     * account actually has.
                                     *
                                     * Do NOT blindly send the user to
                                     * CustomerHomeActivity.
                                     */

                                    if (!selectedRole.equals(
                                            backendRole
                                    )) {

                                        /*
                                         * Do not save the session when
                                         * the requested role does not
                                         * match the account.
                                         */

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "This account is registered as "
                                                        + formatRole(backendRole)
                                                        + ". Please select "
                                                        + formatRole(backendRole)
                                                        + " to continue.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    /*
                                     * -------------------------------------------------
                                     * SAVE SESSION
                                     * -------------------------------------------------
                                     */

                                    sessionManager.saveSession(
                                            body.token,
                                            body.userId,
                                            body.name,
                                            body.email,
                                            backendRole,
                                            body.isVerified
                                    );

                                    /*
                                     * -------------------------------------------------
                                     * ROUTE ACCORDING TO ROLE
                                     * -------------------------------------------------
                                     */

                                    routeToHome(
                                            backendRole
                                    );

                                } else {

                                    /*
                                     * Login failed.
                                     *
                                     * We deliberately do not save
                                     * anything into SessionManager.
                                     */

                                    String message =
                                            getLoginErrorMessage(
                                                    response
                                            );

                                    Toast.makeText(
                                            LoginActivity.this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<AuthModels.AuthResponse> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }

                                setLoading(false);

                                String error =
                                        t.getMessage();

                                if (TextUtils.isEmpty(error)) {

                                    error =
                                            "Unable to connect to the server";
                                }

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Network error: "
                                                + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    /**
     * Get selected role from Spinner.
     */
    private String getSelectedRole() {

        if (roleSpinner == null
                || roleSpinner.getSelectedItem() == null) {

            return "CUSTOMER";
        }

        return normalizeRole(
                String.valueOf(
                        roleSpinner.getSelectedItem()
                )
        );
    }

    /**
     * Normalize backend/frontend role values.
     */
    private String normalizeRole(
            String role
    ) {

        if (role == null) {
            return "";
        }

        String normalized =
                role.trim()
                        .toUpperCase();

        /*
         * Support possible enum formatting such as:
         *
         * User.Role.FARMER
         */
        if (normalized.endsWith(
                ".FARMER"
        )) {

            return "FARMER";
        }

        if (normalized.endsWith(
                ".CUSTOMER"
        )) {

            return "CUSTOMER";
        }

        if (normalized.endsWith(
                ".ADMIN"
        )) {

            return "ADMIN";
        }

        return normalized;
    }

    /**
     * Convert internal role to readable text.
     */
    private String formatRole(
            String role
    ) {

        if ("FARMER".equals(
                normalizeRole(role)
        )) {

            return "Farmer";
        }

        if ("ADMIN".equals(
                normalizeRole(role)
        )) {

            return "Admin";
        }

        return "Customer";
    }

    /**
     * Route authenticated user to the correct dashboard.
     */
    private void routeToHome(
            String role
    ) {

        String normalizedRole =
                normalizeRole(role);

        Intent intent;

        switch (normalizedRole) {

            case "FARMER":

                intent =
                        new Intent(
                                this,
                                FarmerHomeActivity.class
                        );

                break;

            case "ADMIN":

                intent =
                        new Intent(
                                this,
                                AdminDashboardActivity.class
                        );

                break;

            case "CUSTOMER":

                intent =
                        new Intent(
                                this,
                                CustomerHomeActivity.class
                        );

                break;

            default:

                /*
                 * Never silently send an unknown role
                 * to Customer dashboard.
                 */

                Toast.makeText(
                        this,
                        "Unknown account role. Please contact administrator.",
                        Toast.LENGTH_LONG
                ).show();

                sessionManager.logout();

                return;
        }

        /*
         * Prevent the user from pressing Back and returning
         * to LoginActivity after successful login.
         */

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    /**
     * Display a useful login error.
     */
    private String getLoginErrorMessage(
            Response<AuthModels.AuthResponse> response
    ) {

        int code =
                response.code();

        /*
         * Most backend authentication failures
         * will be 401 or 400.
         */

        if (code == 401
                || code == 400) {

            if (response.errorBody() != null) {
                try {
                    String errorBody = response.errorBody().string();
                    if (errorBody.contains("You were blocked by admin")) {
                        return "You were blocked by admin. You are requested to contact Admin for further process.";
                    }
                    if (errorBody.contains("You may login once you have been verified by the admin")
                            || errorBody.contains("under verification")) {
                        return "You may login once you have been verified by the admin.";
                    }
                } catch (Exception ignored) {
                }
            }

            return "Invalid email or password";
        }

        if (code == 403) {

            if (response.errorBody() != null) {
                try {
                    String errorBody = response.errorBody().string();
                    if (errorBody.contains("You were blocked by admin")) {
                        return "You were blocked by admin. You are requested to contact Admin for further process.";
                    }
                } catch (Exception ignored) {
                }
            }

            return "You were blocked by admin. You are requested to contact Admin for further process.";
        }

        if (code >= 500) {

            return "Server error. Please try again later.";
        }

        return "Login failed. Please check your details.";
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkBlockedNotice(intent);
    }

    private void checkBlockedNotice(Intent intent) {
        if (intent == null) return;
        String blockedMsg = intent.getStringExtra("blocked_message");
        if (!TextUtils.isEmpty(blockedMsg)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Account Blocked")
                    .setMessage(blockedMsg)
                    .setCancelable(false)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    /**
     * Enable/disable login controls.
     */
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

        if (loginButton != null) {

            loginButton.setEnabled(
                    !loading
            );
        }

        if (emailInput != null) {

            emailInput.setEnabled(
                    !loading
            );
        }

        if (passwordInput != null) {

            passwordInput.setEnabled(
                    !loading
            );
        }

        if (roleSpinner != null) {

            roleSpinner.setEnabled(
                    !loading
            );
        }

        if (registerLink != null) {

            registerLink.setEnabled(
                    !loading
            );
        }
    }

    private void showForgotPasswordDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, 0);

        TextView instructionText = new TextView(this);
        instructionText.setText("Enter your registered email address. If an account exists, a password reset link will be sent to your email.");
        instructionText.setTextColor(getResources().getColor(R.color.text_secondary));
        instructionText.setTextSize(14);
        container.addView(instructionText);

        com.google.android.material.textfield.TextInputLayout inputLayout =
                new com.google.android.material.textfield.TextInputLayout(this);
        inputLayout.setHint("Email");
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        inputLayout.setLayoutParams(lp);

        com.google.android.material.textfield.TextInputEditText emailField =
                new com.google.android.material.textfield.TextInputEditText(this);
        emailField.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        String currentEmail = emailInput != null ? emailInput.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(currentEmail)) {
            emailField.setText(currentEmail);
            emailField.setSelection(currentEmail.length());
        }

        inputLayout.addView(emailField);
        container.addView(inputLayout);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setView(container)
                .setPositiveButton("Send Reset Link", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button sendBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            sendBtn.setOnClickListener(v -> {
                String email = emailField.getText() != null ? emailField.getText().toString().trim() : "";

                if (TextUtils.isEmpty(email)) {
                    inputLayout.setError("Enter your email");
                    emailField.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    inputLayout.setError("Enter a valid email address");
                    emailField.requestFocus();
                    return;
                }

                inputLayout.setError(null);
                sendBtn.setEnabled(false);
                sendBtn.setText("Sending...");

                apiService.forgotPassword(new AuthModels.ForgotPasswordRequest(email))
                        .enqueue(new Callback<AuthModels.MessageResponse>() {
                            @Override
                            public void onResponse(Call<AuthModels.MessageResponse> call, Response<AuthModels.MessageResponse> response) {
                                if (isFinishing() || isDestroyed()) return;
                                dialog.dismiss();

                                String msg = "If an account exists with this email, a password reset link has been sent.";
                                if (response.isSuccessful() && response.body() != null && response.body().message != null) {
                                    msg = response.body().message;
                                }

                                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Password Reset")
                                        .setMessage(msg)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }

                            @Override
                            public void onFailure(Call<AuthModels.MessageResponse> call, Throwable t) {
                                if (isFinishing() || isDestroyed()) return;
                                sendBtn.setEnabled(true);
                                sendBtn.setText("Send Reset Link");
                                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {

        /*
         * Make sure the Activity does not leave
         * an active loading state behind.
         */

        if (progressBar != null) {

            progressBar.setVisibility(
                    View.GONE
            );
        }

        super.onDestroy();
    }

}