# ── Build ──
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY pom.xml ./
COPY common-dto/pom.xml ./common-dto/
COPY common-security/pom.xml ./common-security/
COPY gateway-service/pom.xml ./gateway-service/

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl common-dto,common-security,gateway-service -am -q 2>/dev/null || true

COPY common-dto/src ./common-dto/src
COPY common-security/src ./common-security/src
COPY gateway-service/src ./gateway-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -pl gateway-service -am -DskipTests -q

# ── Runtime ──
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/gateway-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
