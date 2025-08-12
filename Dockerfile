FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache-friendly copy of build scripts and dependency descriptors
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle

# Pre-fetch dependencies (optional but speeds up subsequent builds)
RUN ./gradlew --no-daemon dependencies || true

# Copy sources and build
COPY src src
RUN chmod +x gradlew && ./gradlew --no-daemon clean build -x test

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy fat jar
COPY --from=build /app/build/libs/TheLegendOfBelga.jar /app/app.jar

# Default port
EXPOSE 7777

# Environment variables for configuration
# PORT: server port (default 7777)
# SEED: optional fixed world seed (long)
ENV PORT=7777

# By default run the multiplayer server; allow overrides via env vars
CMD ["sh", "-c", "java -cp /app/app.jar com.lhamacorp.games.tlob.server.Server ${PORT:-7777} ${SEED:+$SEED}"]
