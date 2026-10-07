package com.farmconnect.android.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the logged-in user's JWT + basic profile info in SharedPreferences
 * so the app stays logged in between launches.
 */
public class SessionManager {

    private static final String PREF_NAME = "farmconnect_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_VERIFIED = "verified";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, long userId, String name, String email, String role, boolean verified) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROLE, role)
                .putBoolean(KEY_VERIFIED, verified)
                .apply();
    }

    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public long getUserId() { return prefs.getLong(KEY_USER_ID, -1); }
    public String getName() { return prefs.getString(KEY_NAME, ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getRole() { return prefs.getString(KEY_ROLE, ""); }
    public boolean isVerified() { return prefs.getBoolean(KEY_VERIFIED, false); }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
