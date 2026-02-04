package com.openclassrooms.tourguide.user;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

public class User {
    private final UUID userId;
    private final String userName;
    private String phoneNumber;
    private String emailAddress;
    private Date latestLocationTimestamp;

    private final List<VisitedLocation> visitedLocations = new CopyOnWriteArrayList<>();
    private final List<UserReward> userRewards = new CopyOnWriteArrayList<>();
    // Ajout d'un verrou dédié pour les opérations critiques sur les récompenses
    private final Object rewardLock = new Object();

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

    public void addToVisitedLocations(VisitedLocation v) {
        visitedLocations.add(v);
        // Limite à 30 éléments pour éviter l'explosion mémoire
        if (visitedLocations.size() > 30) {
            visitedLocations.remove(0); // On retire le plus ancien emplacement visité
        }
    }

    public List<VisitedLocation> getVisitedLocations() {
        return visitedLocations;
    }
    
    public void clearVisitedLocations() {
        visitedLocations.clear();
    }
    
    public void addUserReward(UserReward userReward) {
        // Synchronisation pour garantir qu'on ne crée pas de doublon en concurrence
        synchronized(rewardLock) {
            // Vérification défensive pour s'assurer qu'une récompense pour cette attraction n'existe pas déjà,
            // garantissant l'idempotence de l'ajout de récompense au niveau de l'objet User.
            if(userRewards.stream().noneMatch(r -> r.attraction.attractionName.equals(userReward.attraction.attractionName))) {
                userRewards.add(userReward);
            }
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
        // Attention : cette méthode suppose que la liste n'est jamais vide.
        // Elle lèvera une exception si visitedLocations est vide.
        return visitedLocations.get(visitedLocations.size() - 1);
    }
    
    public void setTripDeals(List<Provider> tripDeals) {
        this.tripDeals = tripDeals;
    }
    
    public List<Provider> getTripDeals() {
        return tripDeals;
    }

}