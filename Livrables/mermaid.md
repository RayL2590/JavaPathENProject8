# TourGuide — Schéma technique (section 3.1)

```mermaid
graph TD
    Client[Client Web / Mobile] --> Controller[TourGuideController<br>REST Endpoints]

    Controller -->|/getLocation<br>/getNearbyAttractions| TGS[TourGuideService]
    Controller -->|/getRewards<br>/getTripDeals| TGS
    Controller -->|/getAllUsersBasic<br>/triggerVisit| TGS

    TGS -->|"trackUserLocation()<br>CompletableFuture"| GPS[GpsUtil]
    TGS -->|"getNearByAttractions()<br>distance + points"| RS[RewardsService]
    TGS -->|"getTripDeals()"| TP[TripPricer]
    TGS -->|"create/start"| Tracker

    RS -->|reward points| RC[RewardCentral]
    RS -->|attractions en mémoire| GPS
    Controller -->|"/triggerVisit (demo)<br/>accès via TGS"| GPS
    Controller -->|"/triggerVisit (demo)<br/>accès via TGS"| RS

    Tracker["Tracker Thread<br>(polling 5 min)"] -->|track + rewards<br>pour tous les users| TGS

    subgraph Memory[Stockage en mémoire]
        UMap[ConcurrentHashMap<br>internalUserMap]
        UData[User<br>CopyOnWriteArrayList<br>visitedLocations / userRewards]
    end

    TGS --> UMap
    UMap --> UData
    RS --> UData

    subgraph Pools[Exécution concurrente]
        TGSPool["TourGuideService<br/>ExecutorService x100"]
        RSPool["RewardsService<br/>ExecutorService x100"]
    end

    TGS --> TGSPool
    RS --> RSPool
```

## Lecture rapide

- **Entrée HTTP** : `TourGuideController`.
- **Orchestration métier** : `TourGuideService`.
- **Calcul des récompenses** : `RewardsService` + `RewardCentral`.
- **Offres de voyage** : `TripPricer`.
- **Tracking périodique** : `Tracker` en arrière-plan.
- **Concurrence** : `CompletableFuture` + pools de threads dédiés.
