FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/hackaton-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar app.jar"]
