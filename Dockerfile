FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/

# Download dependencies
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src/ ./src/

# Build the application
RUN gradle build -x test --no-daemon

# Runtime stage
FROM openjdk:17-slim

WORKDIR /app

# Install dependencies for desktop app (if running in container)
RUN apt-get update && apt-get install -y \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

# Copy built JAR
COPY --from=build /app/build/libs/*.jar ./app.jar

# Create data directories
RUN mkdir -p /app/data /app/logs /app/exports

# Expose ports (for web server if needed)
EXPOSE 8080

# Default to CLI mode
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--help"]

