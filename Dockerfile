FROM node:22-bookworm-slim AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -B verify

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system irene && useradd --system --gid irene irene
COPY --from=backend /app/backend/target/irene-backend-0.0.1-SNAPSHOT.jar app.jar
USER irene
ENV SPRING_PROFILES_ACTIVE=cloud
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
