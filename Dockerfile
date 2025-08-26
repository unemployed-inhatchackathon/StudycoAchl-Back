FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/hackaton-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-Dserver.port=8080", "-jar", "app.jar"]
