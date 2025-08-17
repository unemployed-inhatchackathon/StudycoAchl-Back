package com.studycoAchl.hackaton.DTO;

import lombok.Data;

@Data
public class KakaoTokenResponse {
    private String token_type;
    private String access_token;
    private Integer expires_in;
    private String refresh_token;
    private Integer refresh_token_expires_in;
    private String scope;

    @Data
    public static class OAuthToken {
        private String access_token;
        private String refresh_token;
        private String token_type;
        private int expires_in;
        private String scope;
    }
}
