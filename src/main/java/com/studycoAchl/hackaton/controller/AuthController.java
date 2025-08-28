package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.Entity.User;
import com.studycoAchl.hackaton.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/auth/login/kakao")
    public User kakaoLogin(
            @RequestParam("code") String accessCode,
            HttpServletResponse httpServletResponse) {

        return authService.oAuthLogin(accessCode, httpServletResponse);
    }
}
