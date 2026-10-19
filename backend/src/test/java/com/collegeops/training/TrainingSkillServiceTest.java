package com.collegeops.training;

import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingSkillServiceTest {
    @Mock
    private TrainingSkillRepository skills;
    @Mock
    private InstitutionRepository institutions;
    @InjectMocks
    private TrainingSkillService service;

    @Test
    void listPassesInstitutionScopeAndReturnsPagination() {
        UUID institutionId = UUID.randomUUID();
        when(skills.findAllByInstitution_Id(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        TrainingPageResponse<TrainingSkillResponse> result = service.list(institutionId, 1, 10);

        ArgumentCaptor<UUID> tenant = ArgumentCaptor.forClass(UUID.class);
        verify(skills).findAllByInstitution_Id(tenant.capture(), any());
        assertThat(tenant.getValue()).isEqualTo(institutionId);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.content()).isEmpty();
    }

    @Test
    void createTrimsInputAndUsesTheAuthenticatedInstitution() {
        UUID institutionId = UUID.randomUUID();
        Institution institution = new Institution("Test College", "TEST");
        when(skills.existsByInstitution_IdAndNormalizedName(institutionId, "java"))
                .thenReturn(false);
        when(institutions.findById(institutionId)).thenReturn(Optional.of(institution));
        when(skills.save(any(TrainingSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingSkillResponse result = service.create(institutionId,
                new TrainingSkillRequest(" Java ", " Core language "));

        ArgumentCaptor<TrainingSkill> created = ArgumentCaptor.forClass(TrainingSkill.class);
        verify(skills).save(created.capture());
        assertThat(created.getValue().getInstitution().getId()).isEqualTo(institutionId);
        assertThat(result.name()).isEqualTo("Java");
        assertThat(result.description()).isEqualTo("Core language");
    }

    @Test
    void createRejectsDuplicateSkillNamesWithinInstitution() {
        UUID institutionId = UUID.randomUUID();
        when(skills.existsByInstitution_IdAndNormalizedName(institutionId, "java"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(institutionId,
                new TrainingSkillRequest("Java", null)))
                .isInstanceOf(TrainingRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void listRejectsUnboundedPageSizes() {
        assertThatThrownBy(() -> service.list(UUID.randomUUID(), 0, 101))
                .isInstanceOf(TrainingRequestException.class)
                .hasMessageContaining("between 1 and 100");
    }
}
