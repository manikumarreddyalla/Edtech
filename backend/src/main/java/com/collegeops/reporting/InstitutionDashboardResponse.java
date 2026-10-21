package com.collegeops.reporting;

import java.util.Map;

public record InstitutionDashboardResponse(
        String academicYear,
        Map<String, Long> evidenceStatusCounts,
        long pendingReview,
        long overdue,
        long missingEvidence,
        long activeCohorts,
        long enrolledStudents,
        long completedStudents,
        boolean skillGapMetricsAvailable,
        String skillGapMetricsUnavailableReason) {
}
