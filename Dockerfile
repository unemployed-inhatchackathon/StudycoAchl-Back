FROM openjdk:21-jdk-slim

WORKDIR /app
COPY . .
RUN chmod +x gradlew

# 빌드
RUN ./gradlew clean bootJar --no-daemon

# JAR 파일 존재 확인 및 복사
RUN ls -la build/libs/
RUN cp build/libs/*.jar app.jar

EXPOSE 8080

# 하나의 명령어만 사용
CMD ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar app.jar"]