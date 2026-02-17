package com.invoiceai.service;

import com.invoiceai.dto.request.LoginRequest;
import com.invoiceai.dto.request.RefreshTokenRequest;
import com.invoiceai.dto.request.RegisterRequest;
import com.invoiceai.dto.response.AuthResponse;
import com.invoiceai.dto.response.OrganizationResponse;
import com.invoiceai.dto.response.UserResponse;
import com.invoiceai.exception.BadRequestException;
import com.invoiceai.exception.DuplicateResourceException;
import com.invoiceai.model.Organization;
import com.invoiceai.model.RefreshToken;
import com.invoiceai.model.User;
import com.invoiceai.repository.RefreshTokenRepository;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationService organizationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .build();
        user = userRepository.save(user);

        Organization org = organizationService.createOrganization(request.getOrganizationName(), user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = generateAndStoreRefreshToken(user);

        return AuthResponse.builder()
                .user(toUserResponse(user))
                .organizations(List.of(toOrgResponse(org, "OWNER")))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }

        List<OrganizationResponse> orgs = organizationService.getUserOrganizations(user.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = generateAndStoreRefreshToken(user);

        return AuthResponse.builder()
                .user(toUserResponse(user))
                .organizations(orgs)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired or revoked");
        }

        // Revoke old token (rotation)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        List<OrganizationResponse> orgs = organizationService.getUserOrganizations(user.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = generateAndStoreRefreshToken(user);

        return AuthResponse.builder()
                .user(toUserResponse(user))
                .organizations(orgs)
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private String generateAndStoreRefreshToken(User user) {
        String rawToken = jwtTokenProvider.generateRefreshToken(user.getId());
        String tokenHash = hashToken(rawToken);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private OrganizationResponse toOrgResponse(Organization org, String role) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .plan(org.getPlan().name())
                .role(role)
                .build();
    }
}
