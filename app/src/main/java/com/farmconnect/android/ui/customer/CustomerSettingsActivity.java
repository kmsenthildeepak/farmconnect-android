package com.farmconnect.android.ui.customer;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.farmconnect.android.R;
import com.farmconnect.android.network.ApiClient;

/**
 * Simple customer settings screen reached from the sidebar. No settings
 * screen existed before, per the task's instructions this is intentionally
 * minimal: a local-only notification preference (stored in SharedPreferences,
 * no backend endpoint exists for this yet) plus read-only app/server info.
 */
public class CustomerSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "farmconnect_settings";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        SwitchCompat notificationsSwitch = findViewById(R.id.switchNotifications);
        notificationsSwitch.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true));
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply());

        TextView serverUrl = findViewById(R.id.settingsServerUrl);
        serverUrl.setText(ApiClient.BASE_URL);

        TextView appVersion = findViewById(R.id.settingsAppVersion);
        appVersion.setText(getAppVersionName());
    }

    private String getAppVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName != null ? info.versionName : "-";
        } catch (PackageManager.NameNotFoundException e) {
            return "-";
        }
    }
}
