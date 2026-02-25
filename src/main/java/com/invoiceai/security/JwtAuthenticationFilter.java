package com.invoiceai.security;

import com.invoiceai.model.User;
import com.invoiceai.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        boolean blocked = false;

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserPrincipal principal = UserPrincipal.from(user);
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Block unverified users from non-auth, non-me endpoints
                if (!user.isEmailVerified()) {
                    String contextPath = request.getContextPath();
                    String relativePath = request.getRequestURI().substring(contextPath.length());
                    if (!relativePath.startsWith("/auth/") && !relativePath.equals("/users/me")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"error\":\"EMAIL_NOT_VERIFIED\",\"message\":\"Please verify your email address before accessing this resource.\"}");
                        blocked = true;
                    }
                }
            }

            // Set tenant context from header
            String orgId = request.getHeader("X-Organization-Id");
            if (orgId != null) {
                TenantContext.setCurrentOrgId(UUID.fromString(orgId));
            }
        }

        if (!blocked) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } else {
            TenantContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
