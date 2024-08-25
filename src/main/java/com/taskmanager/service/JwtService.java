package com.taskmanager.service;

import com.taskmanager.domain.JwpToken;
import com.taskmanager.repository.JwpTokenRepository;
import com.taskmanager.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwpTokenRepository jwpTokenRepository;
    private static final JwtUtil jwtUtil = new JwtUtil();
    private final UserService userService;

    public static String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    public void saveJwpToken(JwpToken jwpToken) {
        jwpTokenRepository.save(jwpToken);
    }

    public JwpToken getToken(String token) {
        return jwpTokenRepository.findByToken(token).orElse(null);
    }


    public boolean validateRefreshToken(String token) {
        JwpToken jwpToken = getToken(token);
        if (jwpToken == null || jwpToken.isRevoked() || jwpToken.isExpired()) {
            return false;
        }
        return true;
    }

    public String refreshToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) {
            return null;
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userService.findByEmail(username);
        if (userDetails == null) {
            return null;
        }

        return jwtUtil.generateToken(userDetails);
    }
}
