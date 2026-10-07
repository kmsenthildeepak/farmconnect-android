package com.farmconnect.android.ui.farmer;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.farmconnect.android.R;
import com.farmconnect.android.model.Product;
import com.farmconnect.android.model.ProductRequest;
import com.farmconnect.android.network.ApiClient;
import com.farmconnect.android.network.ApiService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditProductActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {
            "VEGETABLES",
            "FRUITS",
            "GRAINS",
            "ORGANIC_FOODS",
            "OTHER"
    };

    private static final String[] UNITS = {
            "KG",
            "GRAM",
            "LITRE",
            "PIECE",
            "DOZEN"
    };

    private EditText nameInput;
    private EditText descriptionInput;
    private EditText priceInput;
    private EditText quantityInput;

    private Spinner categorySpinner;
    private Spinner unitSpinner;

    private CheckBox organicCheckbox;

    private ImageView productImage;

    private Button saveButton;
    private Button priceSuggestionButton;
    private Button pickImageButton;

    private ProgressBar progressBar;
    private TextView priceSuggestionText;

    private ApiService apiService;

    private long productId = -1;

    private Uri selectedImageUri;
    private File selectedImageFile;

    private String existingImageUrl;

    private boolean activityDestroyed = false;

    /**
     * Resolves a backend image URL/path against the CURRENT backend host
     * (ApiClient.BASE_URL), regardless of which host was baked into it
     * originally. See ImageUrlHelper for why a hardcoded IP here (this file
     * previously had its own copy, out of sync with the other two in
     * ImageUrlHelper and FarmerProductAdapter) is what caused the existing
     * image to look fine right after upload but turn into a placeholder
     * after reconnecting on a different network.
     */
    private String normalizeImageUrl(String url) {
        return com.farmconnect.android.network.ApiClient.getImageUrl(url);
    }

    /**
     * Android image picker.
     */
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri == null || activityDestroyed) {
                            return;
                        }

                        selectedImageUri = uri;

                        Glide.with(AddEditProductActivity.this)
                                .load(uri)
                                .placeholder(
                                        R.drawable.ic_placeholder_produce
                                )
                                .error(
                                        R.drawable.ic_placeholder_produce
                                )
                                .into(productImage);

                        // Copy into an app-private file RIGHT NOW rather
                        // than waiting until Save. ActivityResultContracts
                        // .GetContent() only grants a temporary read
                        // permission on the returned content:// Uri, and
                        // that grant is not guaranteed to still be valid
                        // by the time the farmer finishes filling in the
                        // rest of the form and taps Save (this is what
                        // silently produced an image-less product before:
                        // openInputStream() would throw at save-time,
                        // caught, and the product still got created
                        // without ever surfacing why). Copying immediately
                        // means the actual upload later reads from our own
                        // file, which never expires.
                        selectedImageFile = copyPickedImageToCache(uri);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_add_edit_product
        );

        Toolbar toolbar =
                findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(
                v -> finish()
        );

        /*
         * Find views.
         */
        nameInput =
                findViewById(R.id.productNameInput);

        descriptionInput =
                findViewById(R.id.productDescriptionInput);

        priceInput =
                findViewById(R.id.productPriceInput);

        quantityInput =
                findViewById(R.id.productQuantityInput);

        categorySpinner =
                findViewById(R.id.categorySpinner);

        unitSpinner =
                findViewById(R.id.unitSpinner);

        organicCheckbox =
                findViewById(R.id.organicCheckbox);

        productImage =
                findViewById(R.id.productImagePreview);

        restoreSelectedImageUri(savedInstanceState);

        saveButton =
                findViewById(R.id.saveProductButton);

        priceSuggestionButton =
                findViewById(R.id.priceSuggestionButton);

        pickImageButton =
                findViewById(R.id.pickImageButton);

        progressBar =
                findViewById(R.id.progressBar);

        priceSuggestionText =
                findViewById(R.id.priceSuggestionText);

        /*
         * Retrofit API.
         */
        apiService =
                ApiClient.getApiService(this);

        /*
         * Category spinner.
         */
        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        CATEGORIES
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        categorySpinner.setAdapter(
                categoryAdapter
        );

        /*
         * Unit spinner.
         */
        ArrayAdapter<String> unitAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        UNITS
                );

        unitAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        unitSpinner.setAdapter(
                unitAdapter
        );

        /*
         * Add or edit mode.
         */
        productId =
                getIntent().getLongExtra(
                        "productId",
                        -1
                );

        if (productId != -1) {

            setTitle("Edit Product");

            loadExistingProduct();

        } else {

            setTitle("Add Product");
        }

        /*
         * Choose image.
         */
        pickImageButton.setOnClickListener(
                v -> {

                    if (!activityDestroyed) {
                        imagePicker.launch("image/*");
                    }
                }
        );

        /*
         * Price suggestion.
         */
        priceSuggestionButton.setOnClickListener(
                v -> {

                    if (!activityDestroyed) {
                        fetchPriceSuggestion();
                    }
                }
        );

        /*
         * Save product.
         */
        saveButton.setOnClickListener(
                v -> {

                    if (!activityDestroyed) {
                        saveProduct();
                    }
                }
        );
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Without this, an activity recreation (rotation, low-memory
        // process death and restore, some OEM photo-picker behaviors)
        // between picking an image and tapping Save silently forgets the
        // picked image: Save then proceeds as "no image chosen", succeeds
        // with no error, and the product ends up with no image at all -
        // exactly a "placeholder shown, no error toast" symptom.
        // A File path is trivially restorable and never expires, unlike a
        // content:// Uri's read grant - see copyPickedImageToCache.
        if (selectedImageFile != null) {
            outState.putString("selectedImageFilePath", selectedImageFile.getAbsolutePath());
        }
    }

    /**
     * Copies a just-picked content:// image into this app's own cache
     * directory and returns that file, or null if the copy failed (in
     * which case the picked image is treated as not actually selected -
     * see the caller).
     */
    private File copyPickedImageToCache(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            File tempFile = File.createTempFile("product_upload_", ".jpg", getCacheDir());
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void restoreSelectedImageUri(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String filePath = savedInstanceState.getString("selectedImageFilePath");
        if (filePath == null) {
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        selectedImageFile = file;
        Glide.with(this)
                .load(file)
                .placeholder(R.drawable.ic_placeholder_produce)
                .error(R.drawable.ic_placeholder_produce)
                .into(productImage);
    }

    /**
     * Load an existing product.
     */
    private void loadExistingProduct() {

        if (activityDestroyed) {
            return;
        }

        setLoading(true);

        apiService.getProduct(productId).enqueue(
                new Callback<Product>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<Product> call,
                            @NonNull Response<Product> response) {

                        if (activityDestroyed) {
                            return;
                        }

                        setLoading(false);

                        if (response.isSuccessful()
                                && response.body() != null) {

                            bindProduct(
                                    response.body()
                            );

                        } else {

                            Toast.makeText(
                                    AddEditProductActivity.this,
                                    "Could not load product",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Product> call,
                            @NonNull Throwable t) {

                        if (activityDestroyed) {
                            return;
                        }

                        setLoading(false);

                        Toast.makeText(
                                AddEditProductActivity.this,
                                "Network error: "
                                        + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    /**
     * Bind backend product data to the screen.
     */
    private void bindProduct(Product p) {

        if (activityDestroyed) {
            return;
        }

        nameInput.setText(
                p.productName != null
                        ? p.productName
                        : ""
        );

        descriptionInput.setText(
                p.description != null
                        ? p.description
                        : ""
        );

        priceInput.setText(
                String.valueOf(p.price)
        );

        quantityInput.setText(
                String.valueOf(p.quantity)
        );

        organicCheckbox.setChecked(
                p.isOrganic
        );

        int categoryIndex =
                indexOf(
                        CATEGORIES,
                        p.category
                );

        if (categoryIndex >= 0) {

            categorySpinner.setSelection(
                    categoryIndex,
                    false
            );
        }

        int unitIndex =
                indexOf(
                        UNITS,
                        p.unit
                );

        if (unitIndex >= 0) {

            unitSpinner.setSelection(
                    unitIndex,
                    false
            );
        }

        existingImageUrl =
                p.imageUrl;

        String displayImageUrl =
                normalizeImageUrl(
                        existingImageUrl
                );

        if (displayImageUrl != null) {

            Glide.with(this)
                    .load(displayImageUrl)
                    .placeholder(
                            R.drawable.ic_placeholder_produce
                    )
                    .error(
                            R.drawable.ic_placeholder_produce
                    )
                    .into(productImage);

        } else {

            productImage.setImageResource(
                    R.drawable.ic_placeholder_produce
            );
        }
    }

    /**
     * Find an item in an array.
     */
    private int indexOf(
            String[] array,
            String value) {

        if (value == null) {
            return -1;
        }

        for (int i = 0; i < array.length; i++) {

            if (array[i].equalsIgnoreCase(value)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Fetch AI price suggestion from backend.
     * <p>
     * IMPORTANT:
     * <p>
     * Java Unicode escapes are deliberately used below:
     * <p>
     * \u20B9 = the rupee sign
     * \u2013 = an en dash
     * <p>
     * This prevents mojibake (garbled multi-byte sequences) that can
     * appear when a source file containing literal Unicode characters is
     * saved/compiled with a non-UTF-8 charset.
     */
    private void fetchPriceSuggestion() {

        if (activityDestroyed) {
            return;
        }

        String category =
                String.valueOf(
                        categorySpinner.getSelectedItem()
                );

        String unit =
                String.valueOf(
                        unitSpinner.getSelectedItem()
                );

        boolean organic =
                organicCheckbox.isChecked();

        priceSuggestionButton.setEnabled(false);

        priceSuggestionText.setVisibility(
                View.VISIBLE
        );

        priceSuggestionText.setText(
                "Getting price suggestion..."
        );

        apiService.priceSuggestion(
                category,
                unit,
                organic
        ).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {

                        if (activityDestroyed) {
                            return;
                        }

                        priceSuggestionButton.setEnabled(
                                true
                        );

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            priceSuggestionText.setText(
                                    "Could not fetch price suggestion."
                            );

                            Toast.makeText(
                                    AddEditProductActivity.this,
                                    "Could not fetch price suggestion",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        Object suggested =
                                response.body()
                                        .get("suggestedPrice");

                        Object explanation =
                                response.body()
                                        .get("explanation");

                        /*
                         * Backend returned a price.
                         */
                        if (suggested != null) {

                            String suggestedText =
                                    formatSuggestedPrice(
                                            suggested
                                    );

                            String explanationText =
                                    explanation != null
                                            ? String.valueOf(
                                            explanation
                                    )
                                            : "";

                            priceSuggestionText.setText(
                                    "Suggested: \u20B9"
                                            + suggestedText
                                            + " \u2013 "
                                            + explanationText
                            );

                        } else {

                            /*
                             * Backend has insufficient market data.
                             */
                            String message =
                                    explanation != null
                                            ? String.valueOf(
                                            explanation
                                    )
                                            : "No suggestion available.";

                            priceSuggestionText.setText(
                                    message
                            );
                        }

                        priceSuggestionText.setVisibility(
                                View.VISIBLE
                        );
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Throwable t) {

                        if (activityDestroyed) {
                            return;
                        }

                        priceSuggestionButton.setEnabled(
                                true
                        );

                        priceSuggestionText.setVisibility(
                                View.VISIBLE
                        );

                        priceSuggestionText.setText(
                                "Could not connect to price suggestion service."
                        );

                        Toast.makeText(
                                AddEditProductActivity.this,
                                "Could not fetch price suggestion",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /**
     * Format the suggested price returned by Gson/Retrofit.
     * <p>
     * Examples:
     * <p>
     * 172.5  -> 172.50
     * 172.50 -> 172.50
     */
    private String formatSuggestedPrice(
            Object suggested) {

        if (suggested == null) {
            return "0.00";
        }

        try {

            double value =
                    Double.parseDouble(
                            String.valueOf(
                                    suggested
                            )
                    );

            return String.format(
                    java.util.Locale.US,
                    "%.2f",
                    value
            );

        } catch (Exception ignored) {

            return String.valueOf(
                    suggested
            );
        }
    }

    /**
     * Save product.
     */
    private void saveProduct() {

        if (activityDestroyed) {
            return;
        }

        String name =
                nameInput.getText()
                        .toString()
                        .trim();

        String priceString =
                priceInput.getText()
                        .toString()
                        .trim();

        String quantityString =
                quantityInput.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(name)
                || TextUtils.isEmpty(priceString)
                || TextUtils.isEmpty(quantityString)) {

            Toast.makeText(
                    this,
                    "Please fill in name, price and quantity",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        double price;
        int quantity;

        try {

            price =
                    Double.parseDouble(
                            priceString
                    );

            quantity =
                    Integer.parseInt(
                            quantityString
                    );

        } catch (NumberFormatException e) {

            Toast.makeText(
                    this,
                    "Enter a valid price and quantity",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (price < 0) {

            Toast.makeText(
                    this,
                    "Price cannot be negative",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (quantity < 0) {

            Toast.makeText(
                    this,
                    "Quantity cannot be negative",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ProductRequest request =
                new ProductRequest();

        request.productName =
                name;

        request.description =
                descriptionInput
                        .getText()
                        .toString()
                        .trim();

        request.category =
                String.valueOf(
                        categorySpinner.getSelectedItem()
                );

        request.unit =
                String.valueOf(
                        unitSpinner.getSelectedItem()
                );

        request.price =
                price;

        request.quantity =
                quantity;

        request.isOrganic =
                organicCheckbox.isChecked();

        request.availability =
                true;

        setLoading(true);

        Call<Product> saveCall;

        if (productId == -1) {

            saveCall =
                    apiService.createProduct(
                            request
                    );

        } else {

            saveCall =
                    apiService.updateProduct(
                            productId,
                            request
                    );
        }

        saveCall.enqueue(
                new Callback<Product>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<Product> call,
                            @NonNull Response<Product> response) {

                        if (activityDestroyed) {
                            return;
                        }

                        if (response.isSuccessful()
                                && response.body() != null) {

                            long savedProductId =
                                    response.body()
                                            .productId;

                            if (selectedImageFile != null) {

                                uploadImage(
                                        savedProductId
                                );

                            } else {

                                setLoading(false);

                                Toast.makeText(
                                        AddEditProductActivity.this,
                                        "Product saved successfully",
                                        Toast.LENGTH_SHORT
                                ).show();

                                finish();
                            }

                        } else {

                            setLoading(false);

                            Toast.makeText(
                                    AddEditProductActivity.this,
                                    "Could not save product",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Product> call,
                            @NonNull Throwable t) {

                        if (activityDestroyed) {
                            return;
                        }

                        setLoading(false);

                        Toast.makeText(
                                AddEditProductActivity.this,
                                "Network error: "
                                        + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    /**
     * Upload selected product image.
     *
     * Reads from selectedImageFile - copied into our own cache the moment
     * the image was picked (see copyPickedImageToCache) - rather than
     * re-opening the original content:// Uri here. That re-open was the
     * actual bug: GetContent()'s read grant on the picked Uri is temporary
     * and not guaranteed to still be valid by the time the farmer finishes
     * the rest of the form and taps Save, so this could throw and silently
     * leave the product without an image even though nothing looked wrong
     * to the farmer.
     */
    private void uploadImage(
            long savedProductId) {

        if (activityDestroyed) {
            return;
        }

        if (selectedImageFile == null || !selectedImageFile.exists()) {

            setLoading(false);

            Toast.makeText(
                    this,
                    "Product saved, but the selected image could not be found. Please edit the product and choose the photo again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        RequestBody requestBody =
                RequestBody.create(
                        selectedImageFile,
                        MediaType.parse(
                                "image/*"
                        )
                );

        MultipartBody.Part imagePart =
                MultipartBody.Part.createFormData(
                        "file",
                        selectedImageFile.getName(),
                        requestBody
                );

        apiService
                .uploadProductImage(
                        savedProductId,
                        imagePart
                )
                .enqueue(
                        new Callback<Product>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<Product> call,
                                    @NonNull Response<Product> response) {

                                if (activityDestroyed) {
                                    return;
                                }

                                setLoading(false);

                                if (response.isSuccessful()) {

                                    Toast.makeText(
                                            AddEditProductActivity.this,
                                            "Product and image saved successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } else {

                                    Toast.makeText(
                                            AddEditProductActivity.this,
                                            "Product saved, but image upload failed",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                                finish();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<Product> call,
                                    @NonNull Throwable t) {

                                if (activityDestroyed) {
                                    return;
                                }

                                setLoading(false);

                                Toast.makeText(
                                        AddEditProductActivity.this,
                                        "Product saved, but image upload failed: "
                                                + t.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                                finish();
                            }
                        }
                );
    }

    /**
     * Enable/disable controls during network operations.
     */
    private void setLoading(boolean loading) {

        if (activityDestroyed) {
            return;
        }

        progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        saveButton.setEnabled(
                !loading
        );

        pickImageButton.setEnabled(
                !loading
        );

        priceSuggestionButton.setEnabled(
                !loading
        );

        categorySpinner.setEnabled(
                !loading
        );

        unitSpinner.setEnabled(
                !loading
        );

        organicCheckbox.setEnabled(
                !loading
        );
    }

    /**
     * Clear Spinner focus before Activity destruction.
     */
    private void dismissSpinnerPopups() {

        if (categorySpinner != null) {
            categorySpinner.clearFocus();
        }

        if (unitSpinner != null) {
            unitSpinner.clearFocus();
        }
    }

    @Override
    protected void onDestroy() {

        activityDestroyed = true;

        dismissSpinnerPopups();

        if (productImage != null) {

            try {

                Glide.with(this)
                        .clear(productImage);

            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}






