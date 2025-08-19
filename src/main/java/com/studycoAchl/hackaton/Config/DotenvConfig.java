package com.studycoAchl.hackaton.Config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfig {
    @PostConstruct
    public void loadDotenv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")  // 프로젝트 루트
                    .filename(".env")
                    .ignoreIfMissing()  // 파일이 없어도 오류 안 남
                    .load();

            // .env의 모든 값을 시스템 환경변수로 설정
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
                System.out.println("✅ 환경변수 로드: " + entry.getKey() + "=" +
                        (entry.getKey().contains("KEY") ? entry.getValue().substring(0, 10) + "..." : entry.getValue()));
            });

        } catch (Exception e) {
            System.out.println("⚠️  .env 파일 로드 실패: " + e.getMessage());
        }
    }
}