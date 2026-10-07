package com.farmconnect.android.ui.customer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.farmconnect.android.R;
import com.farmconnect.android.model.UpdateContactRequest;
import com.farmconnect.android.model.UserProfile;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Real editable customer profile, backed by GET /api/users/me and
 * PUT /api/users/me/contact (name/phone). Address fields live on the
 * separate CustomerAddressActivity screen (PUT /api/users/me/address).
 */
public class CustomerProfileActivity extends AppCompatActivity {

    private EditText nameInput, phoneInput;
    private TextView emailText, roleText;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        nameInput = findViewById(R.id.profileNameInput);
        phoneInput = findViewById(R.id.profilePhoneInput);
        emailText = findViewById(R.id.profileEmail);
        roleText = findViewById(R.id.profileRoleValue);
        Button saveButton = findViewById(R.id.saveProfileButton);

        apiService = ApiClient.getApiService(this);

        loadProfile();
        saveButton.setOnClickListener(v -> save());
    }

    private void loadProfile() {
        apiService.getMyProfile().enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CustomerProfileActivity.this,
                            "Could not load profile", Toast.LENGTH_SHORT).show();
                    return;
                }
                UserProfile profile = response.body();
                nameInput.setText(profile.name);
                phoneInput.setText(profile.phone);
                emailText.setText(profile.email);
                roleText.setText(profile.role != null ? profile.role : "CUSTOMER");
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Toast.makeText(CustomerProfileActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void save() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            return;
        }
        if (!phone.matches("^[6-9]\\d{9}$")) {
            phoneInput.setError("Enter a valid 10-digit mobile number");
            Toast.makeText(this, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.updateMyContact(new UpdateContactRequest(name, phone)).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CustomerProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CustomerProfileActivity.this, "Could not save profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Toast.makeText(CustomerProfileActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
