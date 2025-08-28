package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.dto.KakaoDTO;
import com.studycoAchl.hackaton.repository.UserRepository;
import com.studycoAchl.hackaton.util.KakaoUtil;
import com.studycoAchl.hackaton.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoUtil kakaoUtil;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public com.studycoAchl.hackaton.entity.AppUsers oAuthLogin(String accessCode, HttpServletResponse httpServletResponse) {
        KakaoDTO.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
        KakaoDTO.KakaoProfile kakaoProfile = kakaoUtil.requestProfile(oAuthToken);
        String email = kakaoProfile.getKakao_account().getEmail();

        com.studycoAchl.hackaton.entity.AppUsers appUsers = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(kakaoProfile));

        String token = jwtUtil.createAccessToken(appUsers.getEmail(), "USER");
        httpServletResponse.setHeader("Authorization", token);

        return appUsers;
    }

    private com.studycoAchl.hackaton.entity.AppUsers createNewUser(KakaoDTO.KakaoProfile kakaoProfile) {
        com.studycoAchl.hackaton.entity.AppUsers newAppUsers = new com.studycoAchl.hackaton.entity.AppUsers();
        newAppUsers.setEmail(kakaoProfile.getKakao_account().getEmail());
        newAppUsers.setUsername(kakaoProfile.getKakao_account().getProfile().getNickname());
        newAppUsers.setPassword(passwordEncoder.encode("defaultPassword"));
        newAppUsers.setToken("defaultToken");

        return userRepository.save(newAppUsers);
    }
}
