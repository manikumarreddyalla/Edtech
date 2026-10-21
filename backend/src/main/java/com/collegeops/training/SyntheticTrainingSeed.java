package com.collegeops.training;

import com.collegeops.institution.InstitutionRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@Profile("dev")
public class SyntheticTrainingSeed {
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    ApplicationRunner seedSyntheticTrainingPathway(TrainingWorkflowService training, InstitutionRepository institutions) {
        return args -> institutions.findByCodeIgnoreCase("DEMO")
                .ifPresent(institution -> training.seedJavaPathway(institution.getId(), null));
    }
}
