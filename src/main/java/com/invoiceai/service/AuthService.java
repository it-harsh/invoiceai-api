package com.invoiceai.service;

import com.invoiceai.dto.request.*;
import com.invoiceai.dto.response.AuthResponse;
import com.invoiceai.dto.response.MessageResponse;
import com.invoiceai.dto.response.OrganizationResponse;
import com.invoiceai.dto.response.UserResponse;
import com.invoiceai.exception.BadRequestException;
import com.invoiceai.exception.DuplicateResourceException;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.Organization;
import com.invoiceai.model.RefreshToken;
import com.invoiceai.model.User;
import com.invoiceai.model.VerificationToken;
import com.invoiceai.model.enums.TokenType;
import com.invoiceai.repository.RefreshTokenRepository;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.repository.VerificationTokenRepository;
import com.invoiceai.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final OrganizationService organizationService;
    private final EmailNotificationService emailNotificationService;
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

        // Send verification email
        String rawToken = createAndStoreToken(user, TokenType.EMAIL_VERIFICATION, 24);
        emailNotificationService.sendVerificationEmail(user.getEmail(), user.getFullName(), rawToken);

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

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<OrganizationResponse> orgs = organizationService.getUserOrganizations(user.getId());
        return AuthResponse.builder()
                .user(toUserResponse(user))
                .organizations(orgs)
                .build();
    }

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        String tokenHash = hashToken(request.getToken());

        VerificationToken vt = verificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification link"));

        if (vt.getType() != TokenType.EMAIL_VERIFICATION) {
            throw new BadRequestException("Invalid verification link");
        }

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            verificationTokenRepository.delete(vt);
            throw new BadRequestException("Verification link has expired. Please request a new one.");
        }

        User user = vt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationTokenRepository.delete(vt);
        log.info("Email verified for user {}", user.getEmail());

        return new MessageResponse("Email verified successfully");
    }

    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    String rawToken = createAndStoreToken(user, TokenType.EMAIL_VERIFICATION, 24);
                    emailNotificationService.sendVerificationEmail(user.getEmail(), user.getFullName(), rawToken);
                });

        // Always return success (don't reveal if email exists or is already verified)
        return new MessageResponse("If your email is registered and unverified, a verification link has been sent.");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .ifPresent(user -> {
                    String rawToken = createAndStoreToken(user, TokenType.PASSWORD_RESET, 1);
                    emailNotificationService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
                });

        // Always return success (don't reveal if email exists)
        return new MessageResponse("If an account exists with that email, a password reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken());

        VerificationToken vt = verificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));

        if (vt.getType() != TokenType.PASSWORD_RESET) {
            throw new BadRequestException("Invalid reset link");
        }

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            verificationTokenRepository.delete(vt);
            throw new BadRequestException("Reset link has expired. Please request a new one.");
        }

        User user = vt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens (force re-login on all devices)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        verificationTokenRepository.delete(vt);
        log.info("Password reset for user {}", user.getEmail());

        return new MessageResponse("Password reset successfully. Please log in with your new password.");
    }

    private String createAndStoreToken(User user, TokenType type, long hoursToExpire) {
        // Delete existing tokens of this type for the user
        verificationTokenRepository.deleteAllByUserIdAndType(user.getId(), type);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        VerificationToken vt = VerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .type(type)
                .expiresAt(Instant.now().plus(hoursToExpire, ChronoUnit.HOURS))
                .build();
        verificationTokenRepository.save(vt);

        return rawToken;
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
