package com.studycoAchl.hackaton.util;

import com.studycoAchl.hackaton.DTO.KakaoTokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class KakaoUtil {

    private String clientId;     // REST API 키
    private String redirectUri;  // redirect URI

    // 스프링에서 application.yml / properties 로 주입
    public void setClient(String clientId) {
        this.clientId = clientId;
    }

    public void setRedirect(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public KakaoTokenResponse requestToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("code", code);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.getBody(), KakaoTokenResponse.class);
            } else {
                System.out.println("카카오 토큰 요청 실패: " + response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
