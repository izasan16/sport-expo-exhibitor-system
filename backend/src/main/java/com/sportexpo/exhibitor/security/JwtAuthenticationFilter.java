package com.sportexpo.exhibitor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportexpo.exhibitor.auth.AuthErrorCodes;
import com.sportexpo.exhibitor.auth.AuthException;
import com.sportexpo.exhibitor.auth.JwtService;
import com.sportexpo.exhibitor.common.ApiResponse;
import com.sportexpo.exhibitor.users.User;
import com.sportexpo.exhibitor.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return !path.startsWith("/api/")
                || path.equals("/api/auth/login")
                || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX) || authorization.length() <= BEARER_PREFIX.length()) {
            writeAuthError(response, new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token"));
            return;
        }

        try {
            var jwtPrincipal = jwtService.parse(authorization.substring(BEARER_PREFIX.length()).trim());
            var user = userRepository.findById(jwtPrincipal.userId())
                    .orElseThrow(() -> new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token"));
            if (user.isDisabled()) {
                throw new AuthException(AuthErrorCodes.USER_DISABLED, "User disabled");
            }
            var currentUser = toCurrentUser(user);
            var authentication = new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    currentUser.roles().stream().map(SimpleGrantedAuthority::new).toList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AuthException exception) {
            SecurityContextHolder.clearContext();
            writeAuthError(response, exception);
        }
    }

    private CurrentUser toCurrentUser(User user) {
        return new CurrentUser(user.getId(), user.getUsername(), Set.copyOf(user.roleCodes()), user.getExhibitorId());
    }

    private void writeAuthError(HttpServletResponse response, AuthException exception) throws IOException {
        response.setStatus(statusFor(exception).value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(exception.getMessage(), exception.getCode()));
    }

    private HttpStatus statusFor(AuthException exception) {
        return AuthErrorCodes.USER_DISABLED.equals(exception.getCode()) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
    }
}
