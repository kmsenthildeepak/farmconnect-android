package com.farmconnect.android.network;

import android.content.Context;

import com.farmconnect.android.util.ImageUrlHelper;
import com.farmconnect.android.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

/**
 * Builds the Retrofit client used across the whole app.
 *
 * BASE_URL notes:
 *  - This IP changes whenever the backend dev machine joins a different
 *    Wi-Fi/network. Update ONLY this constant when that happens -
 *    everything else (Retrofit calls and product image URLs, via
 *    getImageUrl() below) is derived from it.
 *  - Physical device on the same Wi-Fi -> use your machine's LAN IP.
 *  - Production -> replace with your deployed backend's https:// URL.
 */
public class ApiClient {

    public static final String BASE_URL =
            "https://farmconnect-backend-3gj2.onrender.com/";

    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        return getRetrofit(context).create(ApiService.class);
    }

    /**
     * Centralized image URL resolution (see ImageUrlHelper for the actual
     * logic). Every screen that shows a product image should call this
     * instead of hand-rolling its own IP replacement - that duplication is
     * exactly what caused images to break whenever the backend's IP
     * changed.
     */
    public static String getImageUrl(String rawImageUrl) {
        return ImageUrlHelper.resolve(rawImageUrl);
    }

    private static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context);

            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    String token = sessionManager.getToken();
                    Request requestToProceed = original;
                    if (token != null) {
                        requestToProceed = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                    }
                    Response response = chain.proceed(requestToProceed);
                    if (response.code() == 403) {
                        try {
                            String bodyPreview = response.peekBody(2048).string();
                            if (bodyPreview.contains("You were blocked by admin")) {
                                if (sessionManager.isLoggedIn()) {
                                    sessionManager.logout();
                                    android.content.Intent intent = new android.content.Intent(
                                            context.getApplicationContext(),
                                            com.farmconnect.android.ui.auth.LoginActivity.class
                                    );
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.putExtra("blocked_message",
                                            "You were blocked by admin. You are requested to contact Admin for further process.");
                                    context.getApplicationContext().startActivity(intent);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    return response;
                }
            };

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder().setLenient().create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}






