package com.farmconnect.android.model;

/** Mirrors the backend's UpdateAddressRequest DTO. */
public class UpdateAddressRequest {
    public String address;
    public String city;
    public String state;
    public String pincode;

    public UpdateAddressRequest(String address, String city, String state, String pincode) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }
}
