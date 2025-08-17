package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.domain.User;
import com.studycoAchl.hackaton.DTO.KakaoTokenResponse;
import com.studycoAchl.hackaton.util.KakaoUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoUtil kakaoUtil;

    public User oAuthLogin(String accessCode, HttpServletResponse httpServletResponse) {
        KakaoTokenResponse oAuthToken = kakaoUtil.requestToken(accessCode);

        return new User();
    }
}
