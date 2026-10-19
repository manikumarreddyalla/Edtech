package com.collegeops.config;

import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import com.collegeops.users.UserEntity;
import com.collegeops.users.UserRepository;
import com.collegeops.users.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Configuration
@Profile("dev")
public class DevelopmentBootstrap {

    @Bean
    ApplicationRunner bootstrapDevelopmentAdministrator(
            InstitutionRepository institutionRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.email}") String email,
            @Value("${app.bootstrap.admin.password}") String password) {
        return args -> createBootstrapUser(institutionRepository, userRepository, passwordEncoder, email, password);
    }

    @Transactional
    void createBootstrapUser(InstitutionRepository institutionRepository, UserRepository userRepository,
                             PasswordEncoder passwordEncoder, String email, String password) {
        Institution institution = institutionRepository.findByCodeIgnoreCase("DEMO")
                .orElseGet(() -> institutionRepository.save(
                        new Institution("CollegeOps Development Institution", "DEMO")));

        if (userRepository.existsByInstitution_IdAndEmailIgnoreCase(institution.getId(), email)) {
            return;
        }
        if (email.isBlank() || password.length() < 12) {
            throw new IllegalStateException(
                    "Set BOOTSTRAP_ADMIN_EMAIL and a BOOTSTRAP_ADMIN_PASSWORD of at least 12 characters.");
        }

        UserEntity administrator = new UserEntity(institution, email, "Development Administrator",
                passwordEncoder.encode(password), Set.of(UserRole.INSTITUTION_ADMIN));
        userRepository.save(administrator);
    }
}
