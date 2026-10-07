package com.farmconnect.android.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.farmconnect.android.R;
import com.farmconnect.android.ui.admin.AdminDashboardActivity;
import com.farmconnect.android.ui.auth.LoginActivity;
import com.farmconnect.android.ui.customer.CustomerHomeActivity;
import com.farmconnect.android.ui.farmer.FarmerHomeActivity;
import com.farmconnect.android.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, SPLASH_DELAY_MS);
    }

    private void routeNext() {
        SessionManager session = new SessionManager(this);
        Intent intent;

        if (!session.isLoggedIn()) {
            intent = new Intent(this, LoginActivity.class);
        } else {
            switch (session.getRole()) {
                case "FARMER":
                    intent = new Intent(this, FarmerHomeActivity.class);
                    break;
                case "ADMIN":
                    intent = new Intent(this, AdminDashboardActivity.class);
                    break;
                default:
                    intent = new Intent(this, CustomerHomeActivity.class);
            }
        }
        startActivity(intent);
        finish();
    }
}
