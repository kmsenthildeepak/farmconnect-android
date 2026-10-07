package com.farmconnect.android.ui.customer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.farmconnect.android.R;
import com.farmconnect.android.model.UpdateAddressRequest;
import com.farmconnect.android.model.UserProfile;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reached from the sidebar's "Address" item.
 *
 * Real backend-backed address management via GET/PUT /api/users/me(/address)
 * (see UserController/UserServiceImpl on the backend). The schema has one
 * delivery address per user (address/city/state/pincode columns already on
 * the User entity) rather than a multi-address book, so this screen matches
 * that: view + edit the single saved address, no "add new address" list.
 */
public class CustomerAddressActivity extends AppCompatActivity {

    private EditText addressLine, addressCity, addressState, addressPincode;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_address);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        addressLine = findViewById(R.id.addressLine);
        addressCity = findViewById(R.id.addressCity);
        addressState = findViewById(R.id.addressState);
        addressPincode = findViewById(R.id.addressPincode);
        Button saveButton = findViewById(R.id.saveAddressButton);

        apiService = ApiClient.getApiService(this);

        loadAddress();
        saveButton.setOnClickListener(v -> saveAddress());
    }

    private void loadAddress() {
        apiService.getMyProfile().enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                UserProfile profile = response.body();
                addressLine.setText(profile.address);
                addressCity.setText(profile.city);
                addressState.setText(profile.state);
                addressPincode.setText(profile.pincode);
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Toast.makeText(CustomerAddressActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveAddress() {
        String line = addressLine.getText().toString().trim();
        String city = addressCity.getText().toString().trim();
        String state = addressState.getText().toString().trim();
        String pincode = addressPincode.getText().toString().trim();

        if (line.isEmpty() || city.isEmpty() || state.isEmpty() || pincode.isEmpty()) {
            Toast.makeText(this, "Please fill in every field", Toast.LENGTH_SHORT).show();
            return;
        }

        UpdateAddressRequest req = new UpdateAddressRequest(line, city, state, pincode);
        apiService.updateMyAddress(req).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CustomerAddressActivity.this,
                            "Address saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CustomerAddressActivity.this,
                            "Could not save address", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Toast.makeText(CustomerAddressActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
