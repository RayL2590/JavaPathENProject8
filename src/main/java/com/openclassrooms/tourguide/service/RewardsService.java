package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    // proximity in miles
    private final int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private final RewardCentral rewardsCentral;
    private final List<Attraction> attractions;

    // Utilisation d'un pool de threads fixe important pour gérer le calcul asynchrone des récompenses
    // afin de ne pas bloquer le thread principal lors du traitement massif d'utilisateurs.
    private final ExecutorService executorService = Executors.newFixedThreadPool(1000);
    
    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.rewardsCentral = rewardCentral;
        // Chargement des attractions en mémoire au démarrage pour éviter de les récupérer via GpsUtil à chaque calcul (optimisation de performance).
        this.attractions = gpsUtil.getAttractions();
    }
    
    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }
    
    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }
    
    public void calculateRewards(User user) {
        // Exécution asynchrone pour libérer le thread appelant immédiatement, essentiel pour la scalabilité.
        CompletableFuture.runAsync(() -> {
            List<VisitedLocation> userLocations = user.getVisitedLocations();
            
            for(VisitedLocation visitedLocation : userLocations) {
                for(Attraction attraction : attractions) {
                    // Vérifie si l'utilisateur a déjà reçu une récompense pour cette attraction spécifique
                    // afin d'éviter les doublons et les appels coûteux inutiles vers RewardCentral.
                    if(user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
                        if(nearAttraction(visitedLocation, attraction)) {
                            user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
                        }
                    }
                }
            }
        }, executorService);
    }
    
    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        int attractionProximityRange = 200;
        return !(getDistance(attraction, location) > attractionProximityRange);
    }
    
    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return !(getDistance(attraction, visitedLocation.location) > proximityBuffer);
    }
    
    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }
    
    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        // Calcul de la distance angulaire en utilisant la loi des cosinus.
        // Cette méthode est utilisée pour calculer la distance "à vol d'oiseau" entre deux points sur une sphère.
        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        // Conversion de la distance angulaire en milles nautiques (1 degré = 60 milles nautiques),
        // puis conversion finale en miles terrestres (statute miles).
        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
    }

}