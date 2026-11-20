# =============================================================
#  Image unique : Angular (build prod) servi par Spring Boot
#  Utilisée par Render (voir render.yaml)
# =============================================================

# ---------- 1) Build Angular ----------
FROM node:22-alpine AS frontend
WORKDIR /app
COPY psm-frontend/package.json psm-frontend/package-lock.json ./
RUN npm ci
COPY psm-frontend/ ./
RUN npx ng build --configuration production

# ---------- 2) Build Spring Boot (avec Angular dans static/) ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY psm-backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY psm-backend/src ./src
# application.properties n'est pas versionné : on part du modèle (tout est surchargé par variables d'environnement)
RUN cp -n src/main/resources/application.properties.example src/main/resources/application.properties || true
RUN rm -rf src/main/resources/static && mkdir -p src/main/resources/static
COPY --from=frontend /app/dist/psm-frontend/browser/ ./src/main/resources/static/
RUN mvn -q -B clean package -DskipTests

# ---------- 3) Image d'exécution ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/psm.jar app.jar
ENV SPRING_PROFILES_ACTIVE=render \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
