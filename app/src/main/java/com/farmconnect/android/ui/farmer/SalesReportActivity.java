package com.farmconnect.android.ui.farmer;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.farmconnect.android.R;
import com.farmconnect.android.model.SalesReport;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;
import com.farmconnect.android.util.CurrencyFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fully dynamic, backend-driven Sales Report:
 *  - Four "as of now" period cards (Today/This Week/This Month/This Year),
 *    all from one GET /api/farmer/sales-report call.
 *  - An optional custom date range (Start/End date pickers + View Report),
 *    which re-queries the same endpoint with startDate/endDate query params
 *    and replaces the "summary" section + top-products list below with the
 *    scoped result.
 *
 * Trace: SalesReportActivity -> ApiService.mySalesReport(start, end) ->
 * FarmerController -> SalesReportServiceImpl.mySalesReport(...) ->
 * OrderItemRepository -> DB -> SalesReportResponse -> here.
 */
public class SalesReportActivity extends AppCompatActivity {

    private static final SimpleDateFormat API_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    private TextView summary, products, topProductsEmptyState, rangeLabel;
    private TextView todayOrders, todayRevenue, todayUnits;
    private TextView weeklyOrders, weeklyRevenue, weeklyUnits;
    private TextView monthlyOrders, monthlyRevenue, monthlyUnits;
    private TextView yearlyOrders, yearlyRevenue, yearlyUnits;
    private Button startDateButton, endDateButton, viewReportButton, clearRangeButton;
    private ProgressBar progressBar;

    private ApiService apiService;
    private Calendar startDate;
    private Calendar endDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_report);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        summary = findViewById(R.id.salesSummary);
        products = findViewById(R.id.topProducts);
        topProductsEmptyState = findViewById(R.id.topProductsEmptyState);
        rangeLabel = findViewById(R.id.salesRangeLabel);
        progressBar = findViewById(R.id.salesProgress);

        todayOrders = findViewById(R.id.todayOrders);
        todayRevenue = findViewById(R.id.todayRevenue);
        todayUnits = findViewById(R.id.todayUnits);
        weeklyOrders = findViewById(R.id.weeklyOrders);
        weeklyRevenue = findViewById(R.id.weeklyRevenue);
        weeklyUnits = findViewById(R.id.weeklyUnits);
        monthlyOrders = findViewById(R.id.monthlyOrders);
        monthlyRevenue = findViewById(R.id.monthlyRevenue);
        monthlyUnits = findViewById(R.id.monthlyUnits);
        yearlyOrders = findViewById(R.id.yearlyOrders);
        yearlyRevenue = findViewById(R.id.yearlyRevenue);
        yearlyUnits = findViewById(R.id.yearlyUnits);

        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        viewReportButton = findViewById(R.id.viewReportButton);
        clearRangeButton = findViewById(R.id.clearRangeButton);

        apiService = ApiClient.getApiService(this);

        startDateButton.setOnClickListener(v -> pickDate(true));
        endDateButton.setOnClickListener(v -> pickDate(false));
        viewReportButton.setOnClickListener(v -> loadReport());
        clearRangeButton.setOnClickListener(v -> {
            startDate = null;
            endDate = null;
            startDateButton.setText("Start Date");
            endDateButton.setText("End Date");
            clearRangeButton.setVisibility(View.GONE);
            loadReport();
        });

        loadReport();
    }

    private void pickDate(boolean isStart) {
        Calendar initial = isStart
                ? (startDate != null ? startDate : Calendar.getInstance())
                : (endDate != null ? endDate : Calendar.getInstance());

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth, 0, 0, 0);

            if (isStart) {
                startDate = picked;
                startDateButton.setText(DISPLAY_DATE_FORMAT.format(picked.getTime()));
            } else {
                endDate = picked;
                endDateButton.setText(DISPLAY_DATE_FORMAT.format(picked.getTime()));
            }
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /**
     * Loads the report. If both startDate/endDate are set, validates
     * start <= end (mirroring the backend's own check) before sending the
     * request, then shows the scoped custom-range results. Otherwise loads
     * the all-time summary. Either way the four period cards always come
     * from the same response and always reflect "now".
     */
    private void loadReport() {
        String startParam = null;
        String endParam = null;

        if (startDate != null && endDate != null) {
            if (startDate.after(endDate)) {
                Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
                return;
            }
            startParam = API_DATE_FORMAT.format(startDate.getTime());
            endParam = API_DATE_FORMAT.format(endDate.getTime());
            rangeLabel.setText("Custom range: " + DISPLAY_DATE_FORMAT.format(startDate.getTime())
                    + " - " + DISPLAY_DATE_FORMAT.format(endDate.getTime()));
            clearRangeButton.setVisibility(View.VISIBLE);
        } else {
            rangeLabel.setText("All-time summary");
            clearRangeButton.setVisibility(View.GONE);
        }

        progressBar.setVisibility(View.VISIBLE);
        summary.setVisibility(View.GONE);
        products.setVisibility(View.GONE);
        topProductsEmptyState.setVisibility(View.GONE);
        viewReportButton.setEnabled(false);

        apiService.mySalesReport(startParam, endParam).enqueue(new Callback<SalesReport>() {
            @Override
            public void onResponse(Call<SalesReport> call, Response<SalesReport> response) {
                progressBar.setVisibility(View.GONE);
                viewReportButton.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SalesReportActivity.this,
                            response.code() == 400
                                    ? "Invalid date range"
                                    : "Could not load report", Toast.LENGTH_SHORT).show();
                    return;
                }

                SalesReport report = response.body();
                bindPeriodCards(report);
                bindSummaryAndTopProducts(report);
            }

            @Override
            public void onFailure(Call<SalesReport> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                viewReportButton.setEnabled(true);
                Toast.makeText(SalesReportActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindPeriodCards(SalesReport report) {
        bindPeriod(report.today, todayOrders, todayRevenue, todayUnits);
        bindPeriod(report.weekly, weeklyOrders, weeklyRevenue, weeklyUnits);
        bindPeriod(report.monthly, monthlyOrders, monthlyRevenue, monthlyUnits);
        bindPeriod(report.yearly, yearlyOrders, yearlyRevenue, yearlyUnits);
    }

    private void bindPeriod(SalesReport.PeriodSummary period, TextView ordersView, TextView revenueView, TextView unitsView) {
        if (period == null) {
            ordersView.setText("0 orders");
            revenueView.setText(CurrencyFormatter.format(0));
            unitsView.setText("0 units sold");
            return;
        }
        ordersView.setText(period.orders + " order" + (period.orders == 1 ? "" : "s"));
        revenueView.setText(CurrencyFormatter.format(period.revenue));
        unitsView.setText(period.unitsSold + " units sold");
    }

    private void bindSummaryAndTopProducts(SalesReport report) {
        summary.setVisibility(View.VISIBLE);
        summary.setText("Orders: " + report.totalOrders
                + "\nUnits sold: " + report.totalUnitsSold
                + "\nRevenue: " + CurrencyFormatter.format(report.totalRevenue));

        if (report.topProducts == null || report.topProducts.isEmpty()) {
            products.setVisibility(View.GONE);
            topProductsEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        topProductsEmptyState.setVisibility(View.GONE);
        products.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder("Top products\n\n");
        for (SalesReport.ProductSales p : report.topProducts) {
            String unitLabel = p.unit != null && !p.unit.isEmpty() ? p.unit : "units";
            sb.append(p.productName).append(" - ").append(p.unitsSold).append(" ").append(unitLabel)
                    .append(" - ").append(CurrencyFormatter.format(p.revenue)).append("\n");
        }
        products.setText(sb.toString());
    }
}
