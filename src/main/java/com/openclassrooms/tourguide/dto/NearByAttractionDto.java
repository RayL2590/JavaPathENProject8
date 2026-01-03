package com.openclassrooms.tourguide.dto;

// Ce DTO (Data Transfer Object) est utilisé pour formater la réponse de l'endpoint 'getNearbyAttractions'.
// Il permet d'agréger des données provenant de différentes sources (Attraction, localisation de l'utilisateur, calcul de distance et de récompenses) dans un objet simple et plat, optimisé pour la consommation par le client (frontend/mobile), sans exposer directement toute la complexité des modèles de domaine internes.
public class NearByAttractionDto {
    private String attractionName;
    private double attractionLatitude;
    private double attractionLongitude;
    private double userLatitude;
    private double userLongitude;
    private double distanceInMiles;
    private int rewardPoints;

    public NearByAttractionDto(String attractionName, double attractionLatitude, double attractionLongitude, double userLatitude, double userLongitude, double distanceInMiles, int rewardPoints) {
        this.attractionName = attractionName;
        this.attractionLatitude = attractionLatitude;
        this.attractionLongitude = attractionLongitude;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.distanceInMiles = distanceInMiles;
        this.rewardPoints = rewardPoints;
    }

    public String getAttractionName() { return attractionName; }
    public double getAttractionLatitude() { return attractionLatitude; }
    public double getAttractionLongitude() { return attractionLongitude; }
    public double getUserLatitude() { return userLatitude; }
    public double getUserLongitude() { return userLongitude; }
    public double getDistanceInMiles() { return distanceInMiles; }
    public int getRewardPoints() { return rewardPoints; }
}