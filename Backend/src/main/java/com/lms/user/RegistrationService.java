package com.lms.user;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Year;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.common.domain.AppUser;
import com.lms.common.domain.AppUserRepository;
import com.lms.common.domain.AppUserStatus;
import com.lms.common.domain.Member;
import com.lms.common.domain.MemberRepository;
import com.lms.common.domain.MemberType;
import com.lms.common.domain.MembershipStatus;
import com.lms.common.security.AuditAction;
import com.lms.user.dto.RegistrationForm;

/**
 * Backlog item PB-21: public self-registration.
 *
 * <p>Creates an {@link AppUser} the guest can immediately sign in with, and
 * a {@link Member} row that cannot yet borrow or reserve anything —
 * {@code MembershipStatus} starts at {@link MembershipStatus#Suspended},
 * standing in for "awaiting librarian approval" (there is no {@code
 * Pending} value in the schema; see business-rules.md §8 for why
 * {@code Suspended} is the correct stand-in rather than a schema change).
 *
 * <p><b>No role is ever granted here.</b> This method does not touch
 * {@code UserRoleRepository} or {@code AppUser.roleAssignments} at all —
 * not "hidden from the form", structurally absent, so a guest cannot
 * assign themselves a role under any input. Approving the registration
 * (moving {@code MembershipStatus} to {@code Active}, and separately
 * granting a role if the account is ever promoted to a staff position) is
 * a staff action this pass does not build a screen for; see
 * business-rules.md §8.
 */
@Service
public class RegistrationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUsers;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AppUserRepository appUsers, MemberRepository members, PasswordEncoder passwordEncoder) {
        this.appUsers = appUsers;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
    }

    @AuditAction(action = "CREATE", entity = "AppUser")
    @Transactional
    public AppUser register(RegistrationForm form) {
        if (appUsers.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateRegistrationException("email", "An account with this email already exists.");
        }
        if (members.existsByNationalId(form.getNationalId())) {
            throw new DuplicateRegistrationException("nationalId", "An account with this NIC already exists.");
        }

        AppUser user = new AppUser();
        user.setEmail(form.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setPhone(form.getPhone().trim());
        user.setAddressLine1(form.getAddressLine1().trim());
        user.setAddressLine2(blankToNull(form.getAddressLine2()));
        user.setCity(form.getCity().trim());
        user.setStatus(AppUserStatus.Active);
        user = appUsers.save(user);

        Member member = new Member();
        member.setUser(user);
        member.setMembershipNo(generateUniqueMembershipNo());
        member.setNationalId(form.getNationalId().toUpperCase(java.util.Locale.ROOT));
        // A guest cannot self-declare Academic Staff status (a higher
        // borrowing limit) — every self-registration starts as a Student
        // membership. Promoting one to Academic Staff is a staff action.
        member.setMemberType(MemberType.Student);
        member.setJoinedDate(LocalDate.now());
        member.setExpiryDate(LocalDate.now().plusYears(1));
        member.setMembershipStatus(MembershipStatus.Suspended);
        members.save(member);

        return user;
    }

    /** "MEM-<year>-<6 random digits>", retried on the astronomically unlikely collision. */
    private String generateUniqueMembershipNo() {
        String year = String.valueOf(Year.now().getValue());
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "MEM-" + year + "-" + String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!members.existsByMembershipNo(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique membership number after 10 attempts");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
