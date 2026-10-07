package com.farmconnect.android.model;

public class UpdateFarmerProfileRequest {
    public String farmName;
    public String farmAddress;
    public String farmLocation;
    public Integer experience;
    public String description;

    public UpdateFarmerProfileRequest(String farmName, String farmAddress, String farmLocation,
                                       Integer experience, String description) {
        this.farmName = farmName;
        this.farmAddress = farmAddress;
        this.farmLocation = farmLocation;
        this.experience = experience;
        this.description = description;
    }
}
