package com.farmconnect.android.ui.admin;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmconnect.android.R;
import com.farmconnect.android.adapter.FarmerVerificationAdapter;
import com.farmconnect.android.model.Farmer;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerVerificationActivity extends AppCompatActivity implements FarmerVerificationAdapter.Listener {

    private RecyclerView farmerList;
    private TextView emptyText;
    private FarmerVerificationAdapter adapter;
    private ApiService apiService;
    private final List<Farmer> farmers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_verification);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        farmerList = findViewById(R.id.farmerVerificationList);
        emptyText = findViewById(R.id.emptyVerificationsText);

        apiService = ApiClient.getApiService(this);

        farmerList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FarmerVerificationAdapter(this, farmers, this);
        farmerList.setAdapter(adapter);

        loadPending();
    }

    private void loadPending() {
        apiService.pendingFarmers().enqueue(new Callback<List<Farmer>>() {
            @Override
            public void onResponse(Call<List<Farmer>> call, Response<List<Farmer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    farmers.clear();
                    farmers.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    emptyText.setVisibility(farmers.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Farmer>> call, Throwable t) {
                Toast.makeText(FarmerVerificationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onApprove(Farmer farmer) {
        updateStatus(farmer, "VERIFIED");
    }

    @Override
    public void onReject(Farmer farmer) {
        updateStatus(farmer, "REJECTED");
    }

    private void updateStatus(Farmer farmer, String status) {
        Map<String, String> body = new HashMap<>();
        body.put("status", status);
        apiService.verifyFarmer(farmer.farmerId, body).enqueue(new Callback<Farmer>() {
            @Override
            public void onResponse(Call<Farmer> call, Response<Farmer> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FarmerVerificationActivity.this, farmer.farmerName + " " + status.toLowerCase(), Toast.LENGTH_SHORT).show();
                    loadPending();
                } else {
                    Toast.makeText(FarmerVerificationActivity.this, "Could not update status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Farmer> call, Throwable t) {
                Toast.makeText(FarmerVerificationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
