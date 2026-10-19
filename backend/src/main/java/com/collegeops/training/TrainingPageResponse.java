package com.collegeops.training;

import org.springframework.data.domain.Page;

import java.util.List;

public record TrainingPageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    static <T> TrainingPageResponse<T> from(Page<T> result) {
        return new TrainingPageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
