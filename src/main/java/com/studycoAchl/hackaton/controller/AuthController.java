package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.domain.User;
import com.studycoAchl.hackaton.dto.BaseResponse;
import com.studycoAchl.hackaton.dto.UserResponseDTO;
import com.studycoAchl.hackaton.converter.UserConverter;
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
    public BaseResponse<UserResponseDTO.JoinResultDTO> kakaoLogin(
            @RequestParam("code") String accessCode,
            HttpServletResponse httpServletResponse) {

        User user = authService.oAuthLogin(accessCode, httpServletResponse);
        return BaseResponse.onSuccess(UserConverter.toJoinResultDTO(user));
    }
}
