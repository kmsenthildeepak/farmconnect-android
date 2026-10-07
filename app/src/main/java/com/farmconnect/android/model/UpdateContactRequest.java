package com.farmconnect.android.model;

public class UpdateContactRequest {
    public String name;
    public String phone;

    public UpdateContactRequest(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
