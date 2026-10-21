package com.collegeops.reporting;

import com.collegeops.common.RequestIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Institutional Reporting")
public class ReportingController {
    private final ReportingService reporting;

    public ReportingController(ReportingService reporting) {
        this.reporting = reporting;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR', 'PLACEMENT_OFFICER')")
    @Operation(summary = "Return institution-scoped evidence and training metrics")
    public InstitutionDashboardResponse dashboard(
            JwtAuthenticationToken authentication,
            @RequestParam String academicYear) {
        return reporting.dashboard(RequestIdentity.from(authentication).institutionId(), academicYear);
    }

    @GetMapping(value = "/evidence.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR')")
    @Operation(summary = "Export evidence assignments as a formula-safe CSV")
    public ResponseEntity<byte[]> evidenceCsv(
            JwtAuthenticationToken authentication,
            @RequestParam String academicYear) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        byte[] csv = reporting.exportEvidenceCsv(identity.institutionId(), identity.userId(), academicYear);
        return attachment(csv, "text/csv", "evidence-report-" + academicYear + ".csv");
    }

    @GetMapping(value = "/evidence.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR')")
    @Operation(summary = "Download a PDF summary of accreditation evidence")
    public ResponseEntity<byte[]> evidencePdf(
            JwtAuthenticationToken authentication,
            @RequestParam String academicYear) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        byte[] pdf = reporting.exportEvidencePdf(identity.institutionId(), identity.userId(), academicYear);
        return attachment(pdf, MediaType.APPLICATION_PDF_VALUE, "evidence-report-" + academicYear + ".pdf");
    }

    @GetMapping(value = "/training/cohorts/{cohortId}.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'PLACEMENT_OFFICER')")
    @Operation(summary = "Export training results for an institution-owned cohort")
    public ResponseEntity<byte[]> trainingCohortCsv(
            JwtAuthenticationToken authentication,
            @PathVariable UUID cohortId) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        byte[] csv = reporting.exportTrainingCohortCsv(identity.institutionId(), identity.userId(), cohortId);
        return attachment(csv, "text/csv", "training-cohort-" + cohortId + ".csv");
    }

    private ResponseEntity<byte[]> attachment(byte[] content, String mediaType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mediaType));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
