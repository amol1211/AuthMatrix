# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 as builder

WORKDIR /app
COPY . .

# Move into subdirectory with pom.xml
WORKDIR /app/authmatrix-backend

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/authmatrix-backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
