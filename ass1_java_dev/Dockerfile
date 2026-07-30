# --- Stage 1: Build & Dependencies (Maven/Java) ---
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Optimize layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production artifact
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Minimal Secure Runtime ---
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Harden container security by migrating off root privileges
RUN addgroup -g 1001 -S javasys && \
    adduser -S javauser -u 1001 -G javasys && \
    chown -R javauser:javasys /app

USER javauser

# Copy only the compiled JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

