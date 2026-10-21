package com.collegeops.reporting;

import java.time.Instant;

record TrainingResultRow(
        String cohort,
        String studentNumber,
        String assignment,
        Integer attemptNumber,
        String status,
        Integer score,
        String feedback,
        Instant submittedAt) {
}
