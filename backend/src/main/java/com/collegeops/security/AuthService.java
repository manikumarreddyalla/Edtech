package com.collegeops.security;

import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import com.collegeops.institution.InstitutionStatus;
import com.collegeops.users.UserEntity;
import com.collegeops.users.UserRepository;
import com.collegeops.users.UserRole;
import com.collegeops.users.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long accessTokenTtlSeconds;

    public AuthService(InstitutionRepository institutionRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       @Value("${app.security.jwt.access-token-ttl}") java.time.Duration accessTokenTtl) {
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtlSeconds = accessTokenTtl.toSeconds();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Institution institution = institutionRepository.findByCodeIgnoreCase(request.institutionCode().strip())
                .filter(value -> value.getStatus() == InstitutionStatus.ACTIVE)
                .orElseThrow(InvalidCredentialsException::new);

        UserEntity user = userRepository
                .findByInstitution_IdAndEmailIgnoreCase(institution.getId(), request.email().strip())
                .filter(value -> value.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtlSeconds, ChronoUnit.SECONDS);
        List<String> roles = user.getRoles().stream().map(Enum::name).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("collegeops")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("institution_id", institution.getId().toString())
                .claim("roles", roles)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        AuthUserResponse responseUser = new AuthUserResponse(user.getId(), institution.getId(), user.getEmail(),
                user.getDisplayName(), roles);
        return new AuthResponse(token, "Bearer", expiresAt, responseUser);
    }

    public AuthUserResponse currentUser(UUID userId, UUID institutionId, List<String> roles) {
        UserEntity user = userRepository.findById(userId)
                .filter(value -> value.getInstitution().getId().equals(institutionId))
                .filter(value -> value.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(InvalidCredentialsException::new);
        return new AuthUserResponse(user.getId(), institutionId, user.getEmail(), user.getDisplayName(), roles);
    }
}
