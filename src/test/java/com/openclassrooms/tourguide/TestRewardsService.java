package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

public class TestRewardsService {

    @Test
    public void userGetRewards() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

        // Fixture: on désactive la génération d'utilisateurs internes pour isoler le test (sinon bruit + temps).
        InternalTestHelper.setInternalUserNumber(0);

        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        Attraction attraction = gpsUtil.getAttractions().get(0);
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));

        // Le tracking déclenche des traitements asynchrones: on attend explicitement la fin pour éviter un test flaky.
        tourGuideService.trackUserLocation(user).join();

        // Dans ce scénario, on force le calcul des récompenses pour maîtriser le timing du test.
        rewardsService.calculateRewards(user);

        // Attente bornée: le service peut créditer la récompense via des tâches asynchrones.
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !user.getUserRewards().isEmpty());

        List<UserReward> userRewards = user.getUserRewards();

        // Nettoyage: éviter un thread de tracking vivant après le test (interférences entre tests).
        tourGuideService.tracker.stopTracking();

        assertEquals(1, userRewards.size());
    }

	@Test
	public void isWithinAttractionProximity() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		Attraction attraction = gpsUtil.getAttractions().get(0);
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}

	@Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void nearAllAttractions() throws Exception {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

        rewardsService.setProximityBuffer(Integer.MAX_VALUE); // toutes les attractions sont considérées proches

        InternalTestHelper.setInternalUserNumber(1);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        try {
            User user = tourGuideService.getAllUsers().get(0);

            rewardsService.calculateRewards(user).get(30, TimeUnit.SECONDS); // rend l'assertion déterministe

            List<UserReward> userRewards = tourGuideService.getUserRewards(user);
            assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
        } finally {
            tourGuideService.stop(); // libère tracker + pools même en cas d'échec
        }
    }

}
