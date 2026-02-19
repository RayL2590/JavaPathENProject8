# Technologies

> Java 17  
> Spring Boot 3.X  
> JUnit 5  

# How to have gpsUtil, rewardCentral and tripPricer dependencies available ?

> Run these commands **from the root of the project (where the `libs` folder is located)** :
- mvn install:install-file "-Dfile=libs/gpsUtil.jar" "-DgroupId=gpsUtil" "-DartifactId=gpsUtil" "-Dversion=1.0.0" "-Dpackaging=jar"
- mvn install:install-file "-Dfile=libs/RewardCentral.jar" "-DgroupId=rewardCentral" "-DartifactId=rewardCentral" "-Dversion=1.0.0" "-Dpackaging=jar"
- mvn install:install-file "-Dfile=libs/TripPricer.jar" "-DgroupId=tripPricer" "-DartifactId=tripPricer" "-Dversion=1.0.0" "-Dpackaging=jar"
