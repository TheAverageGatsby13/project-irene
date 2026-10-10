# Backend-only image; Firebase serves the separately built frontend.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B verify

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system irene && useradd --system --gid irene irene
COPY --from=build /build/target/irene-backend-0.0.1-SNAPSHOT.jar app.jar
USER irene
ENV SPRING_PROFILES_ACTIVE=hosted
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
