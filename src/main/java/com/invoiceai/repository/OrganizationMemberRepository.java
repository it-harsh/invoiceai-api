package com.invoiceai.repository;

import com.invoiceai.model.OrganizationMember;
import com.invoiceai.model.enums.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    List<OrganizationMember> findByUserId(UUID userId);
    Optional<OrganizationMember> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);
    List<OrganizationMember> findByOrganizationIdAndRoleIn(UUID organizationId, List<MemberRole> roles);
}
