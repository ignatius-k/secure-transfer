# Runtime image. Build the jar first:  mvn -DskipTests package
# (Building on the host reuses the local Maven cache instead of downloading
#  every dependency inside the image.)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

COPY target/secure-transfer-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
