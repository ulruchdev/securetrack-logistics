# ============================================================
#  CBS Logistics - tracking-service
# ============================================================

# --- Build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dépendances
COPY pom.xml ./
COPY common-dto/pom.xml ./common-dto/
COPY tracking-service/pom.xml ./tracking-service/
RUN mvn tracking-service:help-effective-pom -q -Doutput=/dev/null 2>/dev/null || true

# Build complet
COPY . .
RUN mvn clean package -pl tracking-service -am -DskipTests -B -q

# --- Runtime ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S cbs && adduser -S cbs -G cbs
WORKDIR /app

COPY --from=build /workspace/tracking-service/target/*.jar app.jar

USER cbs
EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
