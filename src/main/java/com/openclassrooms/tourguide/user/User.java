package com.openclassrooms.tourguide.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

public class User {
    private final UUID userId;
    private final String userName;
    private String phoneNumber;
    private String emailAddress;
    private Date latestLocationTimestamp;
    
    // Utilisation de CopyOnWriteArrayList pour garantir la thread-safety.
    // Comme les localisations sont lues (par le Tracker) et écrites (par le GPS) potentiellement en même temps,
    // cette structure évite les ConcurrentModificationException sans verrouillage explicite coûteux lors des itérations.
    private List<VisitedLocation> visitedLocations = new CopyOnWriteArrayList<>();
    
    // Même logique de thread-safety pour les récompenses, qui sont calculées et ajoutées de manière asynchrone par le RewardsService.
    private List<UserReward> userRewards = new CopyOnWriteArrayList<>();
    
    private UserPreferences userPreferences = new UserPreferences();
    private List<Provider> tripDeals = new ArrayList<>();
    public User(UUID userId, String userName, String phoneNumber, String emailAddress) {
        this.userId = userId;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    public String getEmailAddress() {
        return emailAddress;
    }
    
    public void setLatestLocationTimestamp(Date latestLocationTimestamp) {
        this.latestLocationTimestamp = latestLocationTimestamp;
    }
    
    public Date getLatestLocationTimestamp() {
        return latestLocationTimestamp;
    }
    
    public void addToVisitedLocations(VisitedLocation visitedLocation) {
        visitedLocations.add(visitedLocation);
    }
    
    public List<VisitedLocation> getVisitedLocations() {
        return visitedLocations;
    }
    
    public void clearVisitedLocations() {
        visitedLocations.clear();
    }
    
    public void addUserReward(UserReward userReward) {
        // Vérification défensive pour s'assurer qu'une récompense pour cette attraction n'existe pas déjà,
        // garantissant l'idempotence de l'ajout de récompense au niveau de l'objet User.
        if(userRewards.stream().noneMatch(r -> r.attraction.attractionName.equals(userReward.attraction.attractionName))) {
            userRewards.add(userReward);
        }
    }
    
    public List<UserReward> getUserRewards() {
        return userRewards;
    }
    
    public UserPreferences getUserPreferences() {
        return userPreferences;
    }
    
    public void setUserPreferences(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    public VisitedLocation getLastVisitedLocation() {
        return visitedLocations.get(visitedLocations.size() - 1);
    }
    
    public void setTripDeals(List<Provider> tripDeals) {
        this.tripDeals = tripDeals;
    }
    
    public List<Provider> getTripDeals() {
        return tripDeals;
    }

}