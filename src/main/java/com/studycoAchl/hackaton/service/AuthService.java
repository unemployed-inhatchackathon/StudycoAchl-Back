package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.DTO.KakaoDTO;
import com.studycoAchl.hackaton.Entity.User;
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

    public User oAuthLogin(String accessCode, HttpServletResponse httpServletResponse) {
        KakaoDTO.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
        KakaoDTO.KakaoProfile kakaoProfile = kakaoUtil.requestProfile(oAuthToken);
        String email = kakaoProfile.getKakao_account().getEmail();

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(kakaoProfile));

        String token = jwtUtil.createAccessToken(user.getEmail(), "USER");
        httpServletResponse.setHeader("Authorization", token);

        return user;
    }

    private User createNewUser(KakaoDTO.KakaoProfile kakaoProfile) {
        User newUser = new User();
        newUser.setEmail(kakaoProfile.getKakao_account().getEmail());
        newUser.setUsername(kakaoProfile.getKakao_account().getProfile().getNickname());
        newUser.setPassword(passwordEncoder.encode("defaultPassword"));
        newUser.setToken("defaultToken");

        return userRepository.save(newUser);
    }
}
