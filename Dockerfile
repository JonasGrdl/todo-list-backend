# Stufe 1: App bauen (Tests laufen in GitHub Actions, nicht im Docker-Build,
# weil sie Docker/Testcontainers brauchen)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Stufe 2: Schlankes Laufzeit-Image
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Render setzt PORT; die App liest ihn über server.port=${PORT:8080}
EXPOSE 8080
# Speicher an kleine Render-Instanzen (512 MB) anpassen
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
