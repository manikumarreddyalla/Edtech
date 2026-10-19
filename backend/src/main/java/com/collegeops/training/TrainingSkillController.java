package com.collegeops.training;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/training/skills")
@Tag(name = "Training")
public class TrainingSkillController {
    private final TrainingSkillService service;

    public TrainingSkillController(TrainingSkillService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List the authenticated institution's skills")
    public TrainingPageResponse<TrainingSkillResponse> list(
            JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(institutionId(authentication), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TRAINER', 'PLACEMENT_OFFICER')")
    @Operation(summary = "Add a skill to the authenticated institution's training catalog")
    public TrainingSkillResponse create(JwtAuthenticationToken authentication,
                                        @Valid @RequestBody TrainingSkillRequest request) {
        return service.create(institutionId(authentication), request);
    }

    private UUID institutionId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getClaimAsString("institution_id"));
    }
}
