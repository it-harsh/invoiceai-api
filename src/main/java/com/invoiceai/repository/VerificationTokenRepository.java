package com.invoiceai.repository;

import com.invoiceai.model.VerificationToken;
import com.invoiceai.model.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.user.id = :userId AND vt.type = :type")
    void deleteAllByUserIdAndType(UUID userId, TokenType type);
}
