package com.collegeops.reporting;

import java.time.LocalDate;

record ReportRow(
        String criterionCode,
        String criterionTitle,
        String department,
        String owner,
        String academicYear,
        String status,
        LocalDate dueDate) {
}
