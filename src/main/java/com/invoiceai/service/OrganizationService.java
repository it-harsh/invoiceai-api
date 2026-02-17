package com.invoiceai.service;

import com.invoiceai.dto.response.OrganizationResponse;
import com.invoiceai.model.Category;
import com.invoiceai.model.Organization;
import com.invoiceai.model.OrganizationMember;
import com.invoiceai.model.User;
import com.invoiceai.model.enums.MemberRole;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.OrganizationMemberRepository;
import com.invoiceai.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final CategoryRepository categoryRepository;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9-]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-{2,}");

    @Transactional
    public Organization createOrganization(String name, User owner) {
        String slug = generateUniqueSlug(name);

        Organization org = Organization.builder()
                .name(name.trim())
                .slug(slug)
                .owner(owner)
                .build();
        org = organizationRepository.save(org);

        // Add owner as member
        OrganizationMember member = OrganizationMember.builder()
                .user(owner)
                .organization(org)
                .role(MemberRole.OWNER)
                .acceptedAt(Instant.now())
                .build();
        memberRepository.save(member);

        // Seed default categories
        seedDefaultCategories(org);

        return org;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getUserOrganizations(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(member -> OrganizationResponse.builder()
                        .id(member.getOrganization().getId())
                        .name(member.getOrganization().getName())
                        .slug(member.getOrganization().getSlug())
                        .plan(member.getOrganization().getPlan().name())
                        .role(member.getRole().name())
                        .build())
                .toList();
    }

    private void seedDefaultCategories(Organization org) {
        List<Category> defaults = List.of(
                buildCategory(org, "Office Supplies", "#3B82F6", "folder"),
                buildCategory(org, "Travel", "#10B981", "plane"),
                buildCategory(org, "Software & Subscriptions", "#8B5CF6", "monitor"),
                buildCategory(org, "Meals & Entertainment", "#F59E0B", "utensils"),
                buildCategory(org, "Professional Services", "#EF4444", "briefcase"),
                buildCategory(org, "Utilities", "#6366F1", "zap"),
                buildCategory(org, "Marketing", "#EC4899", "megaphone"),
                buildCategory(org, "Other", "#6B7280", "circle")
        );
        categoryRepository.saveAll(defaults);
    }

    private Category buildCategory(Organization org, String name, String color, String icon) {
        return Category.builder()
                .organization(org)
                .name(name)
                .color(color)
                .icon(icon)
                .isDefault(true)
                .build();
    }

    private String generateUniqueSlug(String name) {
        String normalized = Normalizer.normalize(name.toLowerCase().trim(), Normalizer.Form.NFD);
        String slug = NON_ALPHANUMERIC.matcher(normalized.replaceAll("\\s+", "-")).replaceAll("");
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "");

        if (slug.isEmpty()) {
            slug = "org";
        }

        String candidate = slug;
        int counter = 1;
        while (organizationRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + counter++;
        }
        return candidate;
    }
}
