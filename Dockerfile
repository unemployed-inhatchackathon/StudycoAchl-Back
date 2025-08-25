FROM openjdk:21-jdk-slim

WORKDIR /app

# 필요한 파일만 복사 (효율적인 레이어 캐싱)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# 실행 권한 부여 및 빌드
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

# JAR 파일을 찾기 쉬운 이름으로 복사
RUN cp build/libs/*.jar app.jar

EXPOSE 8080

# 포트 환경변수를 사용하도록 설정
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]