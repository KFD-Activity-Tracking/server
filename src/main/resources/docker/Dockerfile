






































FROM gradle:8.5-jdk17 AS gradle_J17

WORKDIR /startup

COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle/ ./gradle


RUN gradle dependencies --no-daemon

#^^^ To not invalidate caches dependencies are run before copy
COPY src/ ./src

RUN gradle clean build -x test --no-daemon


FROM eclipse-temurin:17-jdk-alpine
#FROM openjdk:17-jdk-slim

LABEL authors='LemonOG, MP'
LABEL version='0.1'


WORKDIR /release
COPY --from=gradle_J17 /startup/build/libs/*.jar app.jar


EXPOSE 8765

ENTRYPOINT ["java", "-jar", "app.jar"]





































































