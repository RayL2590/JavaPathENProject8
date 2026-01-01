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
    	return tourGuideService.getUserLocation(getUser(userName));
    }
    
    @RequestMapping("/getNearbyAttractions") 
    public List<NearByAttractionDto> getNearbyAttractions(@RequestParam String userName) {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(tourGuideService.getUser(userName));
        return tourGuideService.getNearByAttractions(visitedLocation, tourGuideService.getUser(userName));
    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }


    // 1. Endpoint pour récupérer la liste de quelques utilisateurs (pour le menu déroulant)
    @RequestMapping("/getAllUsersBasic")
    public List<String> getAllUsersBasic() {
        // On renvoie juste les noms des 10 premiers utilisateurs pour pas surcharger le front
        return tourGuideService.getAllUsers().stream()
                .limit(10)
                .map(User::getUserName)
                .collect(Collectors.toList());
    }

    // 2. Endpoint "triche" pour la démo : force un utilisateur à visiter le 1er truc
    // Cela garantit qu'il aura des récompenses à afficher
    @RequestMapping("/triggerVisit")
    public void triggerVisit(@RequestParam String userName) {
        User user = getUser(userName);
        // On téléporte l'utilisateur sur la première attraction
        Attraction attraction = tourGuideService.getGpsUtil().getAttractions().get(0);
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
        // On lance le calcul des récompenses
        tourGuideService.getRewardsService().calculateRewards(user);
    }

    @RequestMapping("/getAllAttractions")
    public List<Attraction> getAllAttractions() {
        return tourGuideService.getGpsUtil().getAttractions();
    }
   

}