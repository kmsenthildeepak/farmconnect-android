package com.farmconnect.android.model;

public class AuthModels {

    public static class RegisterRequest {
        public String name, email, phone, password, confirmPassword, role;
        public String address, city, state, pincode;
        public Double latitude, longitude;
        public String farmName, farmAddress;
    }

    public static class LoginRequest {
        public String email, password;
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class AuthResponse {
        public String token;
        public long userId;
        public String name, email, role;
        public boolean isVerified;
    }

    public static class ForgotPasswordRequest {
        public String email;
        public ForgotPasswordRequest(String email) {
            this.email = email;
        }
    }

    public static class MessageResponse {
        public String message;
    }
}
