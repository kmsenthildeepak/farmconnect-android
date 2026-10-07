package com.farmconnect.android.ui.farmer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.farmconnect.android.R;
import com.farmconnect.android.model.Farmer;
import com.farmconnect.android.model.UpdateContactRequest;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Real editable farmer profile, backed by:
 *  - GET/PUT /api/farmer/profile (farm name/address/location/experience/description)
 *  - PUT /api/users/me/contact (name/phone - these live on User, not Farmer)
 *
 * Both are real authenticated backend calls; nothing here is stored only
 * in SharedPreferences.
 */
public class FarmerProfileActivity extends AppCompatActivity {

    private EditText nameInput, phoneInput, farmNameInput, farmAddressInput,
            farmLocationInput, experienceInput, descriptionInput;
    private TextView emailText, verificationStatusText;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        nameInput = findViewById(R.id.profileNameInput);
        phoneInput = findViewById(R.id.profilePhoneInput);
        emailText = findViewById(R.id.profileEmail);
        verificationStatusText = findViewById(R.id.profileVerificationStatus);
        farmNameInput = findViewById(R.id.farmNameInput);
        farmAddressInput = findViewById(R.id.farmAddressInput);
        farmLocationInput = findViewById(R.id.farmLocationInput);
        experienceInput = findViewById(R.id.experienceInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        Button saveButton = findViewById(R.id.saveProfileButton);

        apiService = ApiClient.getApiService(this);

        loadProfile();
        saveButton.setOnClickListener(v -> save());
    }

    private void loadProfile() {
        apiService.myFarmerProfile().enqueue(new Callback<Farmer>() {
            @Override
            public void onResponse(Call<Farmer> call, Response<Farmer> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(FarmerProfileActivity.this,
                            "Could not load profile", Toast.LENGTH_SHORT).show();
                    return;
                }
                Farmer f = response.body();
                nameInput.setText(f.farmerName);
                phoneInput.setText(f.phone);
                farmNameInput.setText(f.farmName);
                farmAddressInput.setText(f.farmAddress);
                farmLocationInput.setText(f.farmLocation);
                experienceInput.setText(f.experience != null ? String.valueOf(f.experience) : "");
                descriptionInput.setText(f.description);
                verificationStatusText.setText("Verification status: "
                        + (f.verificationStatus != null ? f.verificationStatus : "-"));
            }

            @Override
            public void onFailure(Call<Farmer> call, Throwable t) {
                Toast.makeText(FarmerProfileActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        apiService.getMyProfile().enqueue(new Callback<com.farmconnect.android.model.UserProfile>() {
            @Override
            public void onResponse(Call<com.farmconnect.android.model.UserProfile> call,
                                    Response<com.farmconnect.android.model.UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    emailText.setText(response.body().email);
                }
            }

            @Override
            public void onFailure(Call<com.farmconnect.android.model.UserProfile> call, Throwable t) {
                // Non-critical - email is read-only display anyway.
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

        UpdateContactRequest contactReq = new UpdateContactRequest(name, phone);
        apiService.updateMyContact(contactReq).enqueue(new Callback<com.farmconnect.android.model.UserProfile>() {
            @Override
            public void onResponse(Call<com.farmconnect.android.model.UserProfile> call,
                                    Response<com.farmconnect.android.model.UserProfile> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(FarmerProfileActivity.this,
                            "Could not save name/phone", Toast.LENGTH_SHORT).show();
                }
                saveFarmDetails();
            }

            @Override
            public void onFailure(Call<com.farmconnect.android.model.UserProfile> call, Throwable t) {
                Toast.makeText(FarmerProfileActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveFarmDetails() {
        String farmName = farmNameInput.getText().toString().trim();
        String farmAddress = farmAddressInput.getText().toString().trim();
        String farmLocation = farmLocationInput.getText().toString().trim();
        String experienceText = experienceInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        Integer experience = null;
        if (!TextUtils.isEmpty(experienceText)) {
            try {
                experience = Integer.parseInt(experienceText);
            } catch (NumberFormatException ignored) {
                // leave null - server keeps whatever it already had
            }
        }

        apiService.updateFarmerProfile(new com.farmconnect.android.model.UpdateFarmerProfileRequest(
                farmName, farmAddress, farmLocation, experience, description
        )).enqueue(new Callback<Farmer>() {
            @Override
            public void onResponse(Call<Farmer> call, Response<Farmer> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FarmerProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                    loadProfile();
                } else {
                    Toast.makeText(FarmerProfileActivity.this, "Could not save farm details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Farmer> call, Throwable t) {
                Toast.makeText(FarmerProfileActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
