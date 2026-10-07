package com.farmconnect.android.model;

/**
 * Mirrors the backend's UserResponse DTO. Used for the "My Addresses" /
 * profile screens (GET /api/users/me, PUT /api/users/me/address).
 */
public class UserProfile {
    public long userId;
    public String name;
    public String email;
    public String phone;
    public String role;
    public String profileImage;
    public String address;
    public String city;
    public String state;
    public String pincode;
    public boolean isVerified;
    public boolean isActive;
    public String createdAt;
}
