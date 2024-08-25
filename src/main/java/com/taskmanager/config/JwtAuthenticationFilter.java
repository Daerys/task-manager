package com.taskmanager.config;

import com.taskmanager.security.JwtUtil;
import com.taskmanager.service.CustomUserDetailsService;
import com.taskmanager.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtils;
    private final CustomUserDetailsService customUserDetails;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (isBearerToken(authHeader)) {
            String jwt = authHeader.substring(7);
            String userEmail = JwtService.extractUsername(jwt);
            if (isAuthenticationRequired(userEmail)) {
                authenticateUser(request, jwt, userEmail);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(HttpServletRequest request, String jwt, String userEmail) {
        UserDetails userDetails = customUserDetails.loadUserByUsername(userEmail);
        if (jwtUtils.isTokenValid(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(token);
        }
    }

    private boolean isAuthenticationRequired(String userEmail) {
        return SecurityContextHolder.getContext().getAuthentication() == null && userEmail != null;
    }

    private boolean isBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }
}
