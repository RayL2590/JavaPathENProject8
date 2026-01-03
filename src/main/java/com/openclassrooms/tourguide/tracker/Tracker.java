package com.openclassrooms.tourguide.tracker;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

// Le Tracker est un service d'arrière-plan (Thread) qui met à jour en continu la localisation des utilisateurs.
public class Tracker extends Thread {
    private Logger logger = LoggerFactory.getLogger(Tracker.class);
    // Intervalle de temps entre deux cycles de mise à jour des positions (5 minutes).
    private static final long TRACKING_POLLING_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    
    // Utilisation d'un pool de threads dédié pour paralléliser les appels de tracking.
    // Un pool large est nécessaire ici car trackUserLocation implique des I/O (appels GPS) qui peuvent être lents.
    private final ExecutorService executorService = Executors.newFixedThreadPool(1000);

    private final TourGuideService tourGuideService;
    private boolean stop = false;

    public Tracker(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    /**
     * Assures to shut down the Tracker thread
     */
    public void stopTracking() {
        stop = true;
        executorService.shutdownNow();
    }

    @Override
    public void run() {
        StopWatch stopWatch = new StopWatch();
        while (true) {
            if (Thread.currentThread().isInterrupted() || stop) {
                logger.debug("Tracker stopping");
                break;
            }

            List<User> users = tourGuideService.getAllUsers();
            logger.debug("Begin Tracker. Tracking " + users.size() + " users.");
            stopWatch.start();

            // Lancement des tâches de tracking en parallèle pour tous les utilisateurs.
            // L'utilisation de CompletableFuture permet de ne pas bloquer la boucle sur chaque utilisateur séquentiellement.
            CompletableFuture<?>[] futures = users.stream()
                .map(u -> CompletableFuture.runAsync(() -> tourGuideService.trackUserLocation(u), executorService))
                .toArray(CompletableFuture[]::new);

            // On attend que toutes les tâches de tracking soient terminées avant de passer au cycle suivant.
            // Cela garantit que le système ne surcharge pas si le tracking prend plus de temps que prévu.
            CompletableFuture.allOf(futures).join();

            stopWatch.stop();
            logger.debug("Tracker Time Elapsed: " + stopWatch.getDuration().toSeconds() + " seconds.");
            stopWatch.reset();
            try {
                logger.debug("Tracker sleeping");
                // Pause du thread avant le prochain cycle de mise à jour.
                TimeUnit.MILLISECONDS.sleep(TRACKING_POLLING_INTERVAL);
            } catch (InterruptedException e) {
                break;
            }
        }

    }
}