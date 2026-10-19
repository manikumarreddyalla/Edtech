package com.collegeops.training;

import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Locale;

@Service
public class TrainingSkillService {
    private final TrainingSkillRepository skills;
    private final InstitutionRepository institutions;

    public TrainingSkillService(TrainingSkillRepository skills, InstitutionRepository institutions) {
        this.skills = skills;
        this.institutions = institutions;
    }

    @Transactional(readOnly = true)
    public TrainingPageResponse<TrainingSkillResponse> list(UUID institutionId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new TrainingRequestException(400, "VALIDATION_ERROR",
                    "Page must be non-negative and size must be between 1 and 100.");
        }
        var result = skills.findAllByInstitution_Id(institutionId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")))
                .map(TrainingSkillResponse::from);
        return TrainingPageResponse.from(result);
    }

    @Transactional
    public TrainingSkillResponse create(UUID institutionId, TrainingSkillRequest request) {
        String name = request.name().strip();
        if (skills.existsByInstitution_IdAndNormalizedName(institutionId, name.toLowerCase(Locale.ROOT))) {
            throw new TrainingRequestException(409, "CONFLICT", "A skill with this name already exists.");
        }
        Institution institution = institutions.findById(institutionId)
                .orElseThrow(() -> new TrainingRequestException(404, "NOT_FOUND", "Institution was not found."));
        TrainingSkill skill = skills.save(new TrainingSkill(institution, name,
                request.description() == null ? null : request.description().strip()));
        return TrainingSkillResponse.from(skill);
    }
}
