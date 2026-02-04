package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import org.awaitility.Awaitility;

public class TestPerformance {

	/*
	 * A note on performance improvements:
	 * 
	 * The number of users generated for the high-volume tests can be easily
	 * adjusted via this method:
	 * 
	 * InternalTestHelper.setInternalUserNumber(100000);
	 * 
	 * 
	 * These tests can be modified to suit new solutions, just as long as the
	 * performance metrics at the end of the tests remain consistent.
	 * 
	 * These are performance metrics that we are trying to hit:
	 * 
	 * highVolumeTrackLocation: 100 thousand users within 15 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 *
	 * highVolumeGetRewards: 100 thousand users within 20 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	@Test
	public void highVolumeTrackLocation() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		// Users should be incremented up to 100,000, and the test finishes within 15 minutes
		InternalTestHelper.setInternalUserNumber(100000);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		List<User> allUsers;
		allUsers = tourGuideService.getAllUsers();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// On lance les tracks en parallèle et on attend la complétion globale :
		// sinon on mesure surtout le coût de soumission des tâches, pas leur exécution.
		CompletableFuture<?>[] futures = allUsers.stream()
				.map(tourGuideService::trackUserLocation)
				.toArray(CompletableFuture[]::new);

		CompletableFuture.allOf(futures).join();

		System.out.println("Nombre d'utilisateurs testés : " + allUsers.size());
		stopWatch.stop();
		// Nettoyage: évite de laisser un thread de tracking tourner après le test (interférences + ressources).
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: "
                + stopWatch.getDuration().toSeconds() + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(15) >= stopWatch.getDuration().toSeconds());
	}

	@Test
	public void highVolumeGetRewards() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Users should be incremented up to 100,000, and the test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(100000);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		Attraction attraction = gpsUtil.getAttractions().get(0);

		// Précondition: on injecte une visite “près” d’une attraction pour rendre le calcul des rewards déterministe.
		List<User> allUsers;
		allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

		// Le calcul peut s’appuyer sur des traitements asynchrones; on attend ensuite la convergence.
		allUsers.forEach(rewardsService::calculateRewards);

		// Attente bornée + polling modéré pour éviter un busy-wait CPU sur un très grand volume d’utilisateurs.
		Awaitility.await()
        .atMost(20, TimeUnit.MINUTES)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(() -> allUsers.stream().noneMatch(u -> u.getUserRewards().isEmpty()));

		System.out.println("Nombre d'utilisateurs testés : " + allUsers.size());
		stopWatch.stop();
		// Nettoyage: évite des effets de bord entre tests et libère les ressources.
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + stopWatch.getDuration().toSeconds()
                + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(20) >= stopWatch.getDuration().toSeconds());
	}

}
