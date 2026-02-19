package com.openclassrooms.tourguide.dto;

// Ce DTO (Data Transfer Object) est utilisé pour formater la réponse de l'endpoint 'getNearbyAttractions'.
// Il permet d'agréger des données provenant de différentes sources (Attraction, localisation de l'utilisateur, calcul de distance et de récompenses) dans un objet simple et plat, optimisé pour la consommation par le client (frontend/mobile), sans exposer directement toute la complexité des modèles de domaine internes.
public record NearByAttractionDto(String attractionName, double attractionLatitude, double attractionLongitude,
                                  double userLatitude, double userLongitude, double distanceInMiles, int rewardPoints) {
}