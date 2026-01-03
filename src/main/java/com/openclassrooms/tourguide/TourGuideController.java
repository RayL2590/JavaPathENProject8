package com.openclassrooms.tourguide;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import com.openclassrooms.tourguide.dto.NearByAttractionDto;

import tripPricer.Provider;

@RestController
public class TourGuideController {

    @Autowired
    TourGuideService tourGuideService;
    
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    /**
     * Retourne la dernière position connue ou déclenche le suivi asynchrone de la position de l'utilisateur.
     * Réponse asynchrone via CompletableFuture.
     */
    @RequestMapping("/getLocation") 
    public CompletableFuture<VisitedLocation> getLocation(@RequestParam String userName) {
        return tourGuideService.getUserLocation(getUser(userName));
    }
    
    /**
     * Retourne les 5 attractions les plus proches de l'utilisateur, avec distance et points potentiels.
     * Réponse asynchrone via CompletableFuture.
     */
    @RequestMapping("/getNearbyAttractions") 
    public CompletableFuture<List<NearByAttractionDto>> getNearbyAttractions(@RequestParam String userName) {
        // Récupère la position asynchrone, puis calcule les attractions proches
        User user = getUser(userName);
        return tourGuideService.getUserLocation(user)
            .thenApply(visitedLocation -> tourGuideService.getNearByAttractions(visitedLocation, user));
    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
        return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
        // Calcule les offres de voyage en fonction des préférences de l'utilisateur et de ses points de récompense cumulés.
        return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
        return tourGuideService.getUser(userName);
    }


    /**
     * Endpoint pour récupérer la liste de quelques utilisateurs (pour le menu déroulant du frontend).
     * Limité à 10 utilisateurs pour éviter de surcharger l'interface.
     */
    @RequestMapping("/getAllUsersBasic")
    public List<String> getAllUsersBasic() {
        // Limitation volontaire à 10 utilisateurs pour éviter de surcharger le frontend lors du chargement de la liste,
        // car le système peut contenir des milliers d'utilisateurs en mémoire.
        return tourGuideService.getAllUsers().stream()
                .limit(10)
                .map(User::getUserName)
                .collect(Collectors.toList());
    }

    /**
     * Endpoint de test/démo : force un utilisateur à visiter la première attraction pour générer des récompenses.
     */
    @RequestMapping("/triggerVisit")
    public void triggerVisit(@RequestParam String userName) {
        User user = getUser(userName);
        // On téléporte l'utilisateur sur la première attraction
        Attraction attraction = tourGuideService.getGpsUtil().getAttractions().get(0);
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
        
        // Force manuellement le calcul des récompenses immédiatement pour permettre de tester 
        // le flux complet sans attendre le déplacement GPS réel ou le cycle du Tracker.
        tourGuideService.getRewardsService().calculateRewards(user);
    }
   
}