package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import com.openclassrooms.tourguide.dto.NearByAttractionDto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
    private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    public final Tracker tracker;
    boolean testMode = true;

    private final ExecutorService executorService = Executors.newFixedThreadPool(1000);

    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;
        
        // Fixe la locale par défaut à US pour assurer une cohérence dans les formats de nombres et de devises,
        // ce qui est important pour la compatibilité avec les bibliothèques externes comme TripPricer.
        Locale.setDefault(Locale.US);

        if (testMode) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            // Initialisation de données fictives pour le développement et les tests sans base de données réelle.
            initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        // Le Tracker est un thread d'arrière-plan qui met à jour périodiquement la position des utilisateurs.
        tracker = new Tracker(this);
        addShutDownHook();
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    /**
     * Récupère la dernière position connue de l'utilisateur si elle existe,
     * sinon déclenche un suivi asynchrone de la position via GPS.
     * Utilise CompletableFuture pour permettre un traitement non bloquant,
     * ce qui améliore la scalabilité du service.
     *
     * @param user L'utilisateur dont on veut la position.
     * @return Un CompletableFuture contenant la dernière position visitée.
     */
    public CompletableFuture<VisitedLocation> getUserLocation(User user) {
        if (user.getVisitedLocations().size() > 0) {
            return CompletableFuture.completedFuture(user.getLastVisitedLocation());
        } else {
            return trackUserLocation(user);
        }
    }

    public User getUser(String userName) {
        return internalUserMap.get(userName);
    }

    public List<User> getAllUsers() {
        return internalUserMap.values().stream().collect(Collectors.toList());
    }

    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    public List<Provider> getTripDeals(User user) {
        // Calcul du total des points de récompense pour obtenir de meilleures offres.
        int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
        
        // Appel à la librairie externe TripPricer pour obtenir des offres basées sur les préférences et les points de l'utilisateur.
        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
                user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
                user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
        user.setTripDeals(providers);
        return providers;
    }

    public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
        // On enveloppe l'appel bloquant dans supplyAsync
        return CompletableFuture.supplyAsync(() -> {
            return gpsUtil.getUserLocation(user.getUserId());
        }, executorService) // On utilise un petit pool partagé
        .thenApply(visitedLocation -> {
            user.addToVisitedLocations(visitedLocation);
            return visitedLocation;
        })
        .thenApply(visitedLocation -> {
            // On enchaîne le calcul des récompenses sans bloquer
            rewardsService.calculateRewards(user);
            return visitedLocation;
        });
    }

    public List<NearByAttractionDto> getNearByAttractions(VisitedLocation visitedLocation, User user) {
        List<NearByAttractionDto> nearbyAttractions = new ArrayList<>();
        
        // On récupère toutes les attractions et on les trie par distance croissante par rapport à l'utilisateur.
        gpsUtil.getAttractions().stream()
            .sorted((a1, a2) -> {
                double dist1 = rewardsService.getDistance(a1, visitedLocation.location);
                double dist2 = rewardsService.getDistance(a2, visitedLocation.location);
                return Double.compare(dist1, dist2);
            })
            .limit(5) // On ne garde que les 5 plus proches pour l'affichage.
            .forEach(attraction -> {
                double distance = rewardsService.getDistance(attraction, visitedLocation.location);
                // Récupération des points potentiels pour cette attraction spécifique.
                int rewardPoints = rewardsService.getRewardPoints(attraction, user);
                
                nearbyAttractions.add(new NearByAttractionDto(
                attraction.attractionName,
                attraction.latitude,                
                attraction.longitude,               
                visitedLocation.location.latitude,  
                visitedLocation.location.longitude, 
                distance,
                rewardPoints
                ));
            });

        return nearbyAttractions;
    }

    private void addShutDownHook() {
        // Assure que le thread du Tracker est arrêté proprement lors de l'arrêt de l'application JVM.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                tracker.stopTracking();
            }
        });
    }

    /**********************************************************************************
     * 
     * Methods Below: For Internal Testing
     * 
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes
    // internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
    }

    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                    new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

    public GpsUtil getGpsUtil() {
        return gpsUtil;
    }

    public RewardsService getRewardsService() {
        return rewardsService;
    }

}