package com.openclassrooms.tourguide;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) {
        // Délègue au service la récupération de la dernière position connue ou le déclenchement d'un nouveau tracking GPS.
        return tourGuideService.getUserLocation(getUser(userName));
    }
    
    @RequestMapping("/getNearbyAttractions") 
    public List<NearByAttractionDto> getNearbyAttractions(@RequestParam String userName) {
        // Récupération de la position actuelle pour calculer les distances.
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(tourGuideService.getUser(userName));
        // Retourne une liste de DTOs contenant les 5 attractions les plus proches,
        // leurs distances et les points de récompense potentiels, formatés spécifiquement pour l'affichage client.
        return tourGuideService.getNearByAttractions(visitedLocation, tourGuideService.getUser(userName));
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


    // 1. Endpoint pour récupérer la liste de quelques utilisateurs (pour le menu déroulant)
    @RequestMapping("/getAllUsersBasic")
    public List<String> getAllUsersBasic() {
        // Limitation volontaire à 10 utilisateurs pour éviter de surcharger le frontend lors du chargement de la liste,
        // car le système peut contenir des milliers d'utilisateurs en mémoire.
        return tourGuideService.getAllUsers().stream()
                .limit(10)
                .map(User::getUserName)
                .collect(Collectors.toList());
    }

    // 2. Endpoint "triche" pour la démo : force un utilisateur à visiter la premiere attraction
    // Cela garantit qu'il aura des récompenses à afficher
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

    @RequestMapping("/getAllAttractions")
    public List<Attraction> getAllAttractions() {
        return tourGuideService.getGpsUtil().getAttractions();
    }
   

}