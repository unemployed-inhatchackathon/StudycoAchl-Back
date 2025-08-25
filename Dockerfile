FROM openjdk:21-jdk-slim

WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

EXPOSE 8080
CMD ["sh", "-c", "java -Dserver.port=$PORT -jar build/libs/*.jar"]