# 베이스 이미지: JDK 21
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리
WORKDIR /app

# JAR 파일 복사
COPY build/libs/hackaton-0.0.1-SNAPSHOT.jar app.jar

# 컨테이너에서 8080 포트 열기
EXPOSE 8080

# Cloud Run에서 동적 포트를 사용하도록 설정
# PORT 환경변수를 Spring Boot에 전달
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar /app/app.jar"]
