# Use a lightweight OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy built JAR from host to container
COPY target/authmatrix-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port (optional - Render handles ports automatically)
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
