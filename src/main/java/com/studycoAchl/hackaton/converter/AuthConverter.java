package com.studycoAchl.hackaton.converter;

import com.studycoAchl.hackaton.Entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthConverter {

    public static User toUser(String email, String name, String password, PasswordEncoder passwordEncoder) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(name);
        newUser.setPassword(password != null ? passwordEncoder.encode(password) : null);
        newUser.setToken("defaultToken");
        return newUser;
    }
}
