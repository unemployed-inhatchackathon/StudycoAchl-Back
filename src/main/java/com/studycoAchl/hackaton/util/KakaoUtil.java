package com.studycoAchl.hackaton.util;

import com.studycoAchl.hackaton.DTO.KakaoDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoUtil {

    private String clientId;
    private String redirectUri;

    public void setClient(String clientId) {
        this.clientId = clientId;
    }

    public void setRedirect(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    // Access Token 요청
    public KakaoDTO.OAuthToken requestToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.getBody(), KakaoDTO.OAuthToken.class);
            } else {
                System.out.println("카카오 토큰 요청 실패: " + response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 사용자 프로필 요청
    public KakaoDTO.KakaoProfile requestProfile(KakaoDTO.OAuthToken oAuthToken) {
        RestTemplate restTemplate2 = new RestTemplate();
        HttpHeaders headers2 = new HttpHeaders();

        headers2.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        headers2.add("Authorization", "Bearer " + oAuthToken.getAccess_token());

        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest = new HttpEntity<>(headers2);

        try {
            ResponseEntity<String> response2 = restTemplate2.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    kakaoProfileRequest,
                    String.class
            );

            if (response2.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response2.getBody(), KakaoDTO.KakaoProfile.class);
            } else {
                System.out.println("카카오 프로필 요청 실패: " + response2.getStatusCode());
                return null;
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
