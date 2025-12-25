package com.openclassrooms.tourguide.dto;

import gpsUtil.location.Location;

public class NearByAttractionDto {
    private String attractionName;
    private Location attractionLocation;
    private Location userLocation;
    private double distanceInMiles;
    private int rewardPoints;

    public NearByAttractionDto(String attractionName, Location attractionLocation, Location userLocation, double distanceInMiles, int rewardPoints) {
        this.attractionName = attractionName;
        this.attractionLocation = attractionLocation;
        this.userLocation = userLocation;
        this.distanceInMiles = distanceInMiles;
        this.rewardPoints = rewardPoints;
    }

    // Getters
    public String getAttractionName() { return attractionName; }
    public Location getAttractionLocation() { return attractionLocation; }
    public Location getUserLocation() { return userLocation; }
    public double getDistanceInMiles() { return distanceInMiles; }
    public int getRewardPoints() { return rewardPoints; }
}
