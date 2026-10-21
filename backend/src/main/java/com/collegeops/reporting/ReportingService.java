package com.collegeops.reporting;

import com.collegeops.common.AuditEvent;
import com.collegeops.common.AuditEventRepository;
import com.collegeops.common.InvalidRequestException;
import com.collegeops.common.ResourceNotFoundException;
import com.collegeops.accreditation.EvidenceStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportingService {
    private static final int MAX_EXPORT_ROWS = 10_000;
    private static final String SKILL_GAP_UNAVAILABLE =
            "Student skill levels are not recorded against job-role targets yet.";

    private final JdbcTemplate jdbcTemplate;
    private final AuditEventRepository auditEvents;

    public ReportingService(JdbcTemplate jdbcTemplate, AuditEventRepository auditEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public InstitutionDashboardResponse dashboard(UUID institutionId, String academicYear) {
        String year = validateAcademicYear(academicYear);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (EvidenceStatus status : EvidenceStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }
        jdbcTemplate.query(
                """
                select status, count(*) as total
                from evidence_assignments
                where institution_id = ? and academic_year = ?
                group by status
                """,
                result -> {
                    statusCounts.put(result.getString("status"), result.getLong("total"));
                },
                institutionId, year);

        long pendingReview = scalarCount(
                "select count(*) from evidence_assignments where institution_id = ? and academic_year = ? and status = 'SUBMITTED'",
                institutionId, year);
        long overdue = scalarCount(
                """
                select count(*) from evidence_assignments
                where institution_id = ? and academic_year = ? and due_date < current_date and status <> 'VERIFIED'
                """,
                institutionId, year);
        long missingEvidence = scalarCount(
                """
                select count(*) from evidence_assignments
                where institution_id = ? and academic_year = ?
                  and status in ('NOT_STARTED', 'IN_PROGRESS', 'NEEDS_REVISION')
                """,
                institutionId, year);
        long activeCohorts = scalarCount(
                "select count(*) from training_cohorts where institution_id = ? and status = 'ACTIVE'",
                institutionId);
        long enrolledStudents = scalarCount(
                """
                select count(distinct enrollment.student_id)
                from training_enrollments enrollment
                join training_cohorts cohort
                  on cohort.id = enrollment.cohort_id and cohort.institution_id = enrollment.institution_id
                where enrollment.institution_id = ? and enrollment.status = 'ENROLLED'
                  and cohort.status = 'ACTIVE'
                """,
                institutionId);
        long completedStudents = scalarCount(
                """
                select count(distinct student_id) from training_enrollments
                where institution_id = ? and status = 'COMPLETED'
                """,
                institutionId);

        return new InstitutionDashboardResponse(year, Map.copyOf(statusCounts), pendingReview, overdue,
                missingEvidence, activeCohorts, enrolledStudents, completedStudents, false, SKILL_GAP_UNAVAILABLE);
    }

    @Transactional
    public byte[] exportEvidenceCsv(UUID institutionId, UUID actorId, String academicYear) {
        String year = validateAcademicYear(academicYear);
        List<ReportRow> rows = evidenceRows(institutionId, year);
        StringBuilder csv = new StringBuilder("criterion_code,criterion_title,department,owner,academic_year,status,due_date\r\n");
        for (ReportRow row : rows) {
            appendCsvRow(csv, List.of(
                    row.criterionCode(), row.criterionTitle(), row.department(), row.owner(), row.academicYear(),
                    row.status(), row.dueDate() == null ? "" : row.dueDate().toString()));
        }
        auditExport(institutionId, actorId, "EVIDENCE_CSV_EXPORTED", year);
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional
    public byte[] exportEvidencePdf(UUID institutionId, UUID actorId, String academicYear) {
        String year = validateAcademicYear(academicYear);
        List<ReportRow> rows = evidenceRows(institutionId, year);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PdfPageWriter writer = new PdfPageWriter(document, regular, bold);
            writer.write("Accreditation Evidence Report", true);
            writer.write("Academic year: " + year, false);
            writer.write("Generated: " + LocalDate.now(), false);
            writer.write("", false);
            for (EvidenceStatus status : EvidenceStatus.values()) {
                long count = rows.stream().filter(row -> row.status().equals(status.name())).count();
                writer.write(status.name() + ": " + count, false);
            }
            writer.write("", false);
            writer.write("Criterion code | Status | Due date", true);
            writer.write("------------------------------------", false);
            for (ReportRow row : rows) {
                writer.write(row.criterionCode() + " | " + row.status() + " | "
                        + (row.dueDate() == null ? "No due date" : row.dueDate()), false);
            }
            writer.finish();
            document.save(output);
            auditExport(institutionId, actorId, "EVIDENCE_PDF_EXPORTED", year);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not generate the accreditation evidence PDF.", exception);
        }
    }

    @Transactional
    public byte[] exportTrainingCohortCsv(UUID institutionId, UUID actorId, UUID cohortId) {
        boolean belongsToInstitution = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists(select 1 from training_cohorts where id = ? and institution_id = ?)",
                Boolean.class, cohortId, institutionId));
        if (!belongsToInstitution) {
            throw new ResourceNotFoundException("Training cohort");
        }
        List<TrainingResultRow> rows = trainingRows(institutionId, cohortId);
        StringBuilder csv = new StringBuilder(
                "cohort,student_number,assignment,attempt,status,score,feedback,submitted_at\r\n");
        for (TrainingResultRow row : rows) {
            appendCsvRow(csv, List.of(
                    row.cohort(), row.studentNumber(), row.assignment(),
                    row.attemptNumber() == null ? "" : row.attemptNumber().toString(),
                    row.status() == null ? "NOT_SUBMITTED" : row.status(),
                    row.score() == null ? "" : row.score().toString(),
                    row.feedback() == null ? "" : row.feedback(),
                    row.submittedAt() == null ? "" : row.submittedAt().toString()));
        }
        auditExport(institutionId, actorId, "TRAINING_COHORT_CSV_EXPORTED", cohortId.toString());
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<ReportRow> evidenceRows(UUID institutionId, String academicYear) {
        List<ReportRow> rows = jdbcTemplate.query(
                """
                select c.code as criterion_code, c.title as criterion_title,
                       coalesce(d.name, '') as department, u.display_name as owner,
                       e.academic_year, e.status, e.due_date
                from evidence_assignments e
                join accreditation_criteria c on c.id = e.criterion_id and c.institution_id = e.institution_id
                left join departments d on d.id = e.department_id and d.institution_id = e.institution_id
                join users u on u.id = e.owner_user_id and u.institution_id = e.institution_id
                where e.institution_id = ? and e.academic_year = ?
                order by c.code, e.id
                limit ?
                """,
                (result, rowNumber) -> new ReportRow(
                        result.getString("criterion_code"),
                        result.getString("criterion_title"),
                        result.getString("department"),
                        result.getString("owner"),
                        result.getString("academic_year"),
                        result.getString("status"),
                        result.getObject("due_date", LocalDate.class)),
                institutionId, academicYear, MAX_EXPORT_ROWS + 1);
        requireWithinExportLimit(rows.size());
        return rows;
    }

    private List<TrainingResultRow> trainingRows(UUID institutionId, UUID cohortId) {
        List<TrainingResultRow> rows = jdbcTemplate.query(
                """
                select c.name as cohort, s.student_number, coalesce(a.title, '') as assignment,
                       attempt.attempt_number, attempt.status, attempt.score, attempt.feedback,
                       attempt.submitted_at
                from training_cohorts c
                join training_enrollments enrollment
                  on enrollment.cohort_id = c.id and enrollment.institution_id = c.institution_id
                join students s on s.id = enrollment.student_id and s.institution_id = enrollment.institution_id
                left join training_assessment_attempts attempt
                  on attempt.student_id = enrollment.student_id
                 and attempt.institution_id = enrollment.institution_id
                left join training_assignments a
                  on a.id = attempt.assignment_id
                 and a.cohort_id = c.id
                 and a.institution_id = c.institution_id
                where c.id = ? and c.institution_id = ?
                order by s.student_number, a.title, attempt.attempt_number
                limit ?
                """,
                (result, rowNumber) -> new TrainingResultRow(
                        result.getString("cohort"),
                        result.getString("student_number"),
                        result.getString("assignment"),
                        (Integer) result.getObject("attempt_number"),
                        result.getString("status"),
                        (Integer) result.getObject("score"),
                        result.getString("feedback"),
                        result.getObject("submitted_at", java.time.OffsetDateTime.class) == null
                                ? null
                                : result.getObject("submitted_at", java.time.OffsetDateTime.class).toInstant()),
                cohortId, institutionId, MAX_EXPORT_ROWS + 1);
        requireWithinExportLimit(rows.size());
        return rows;
    }

    private long scalarCount(String sql, Object... parameters) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, parameters);
        return count == null ? 0 : count;
    }

    private String validateAcademicYear(String academicYear) {
        if (academicYear == null || !academicYear.matches("\\d{4}(?:-\\d{2,4})?")) {
            throw new InvalidRequestException("Academic year must use YYYY or YYYY-YY format.");
        }
        return academicYear;
    }

    private void requireWithinExportLimit(int rowCount) {
        if (rowCount > MAX_EXPORT_ROWS) {
            throw new InvalidRequestException("The report exceeds the 10,000-row export limit.");
        }
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }
            String value = protectSpreadsheetFormula(values.get(index));
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private String protectSpreadsheetFormula(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int firstVisible = 0;
        while (firstVisible < value.length() && Character.isWhitespace(value.charAt(firstVisible))) {
            firstVisible++;
        }
        if (firstVisible < value.length() && "=+-@".indexOf(value.charAt(firstVisible)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    private void auditExport(UUID institutionId, UUID actorId, String action, String summary) {
        auditEvents.save(new AuditEvent(institutionId, actorId, action, "Report", UUID.randomUUID(), summary));
    }

    private final class PdfPageWriter {
        private final PDDocument document;
        private final PDType1Font regular;
        private final PDType1Font bold;
        private PDPageContentStream content;
        private float y;

        private PdfPageWriter(PDDocument document, PDType1Font regular, PDType1Font bold) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            newPage();
        }

        private void write(String text, boolean heading) throws IOException {
            if (y < 50) {
                newPage();
            }
            content.beginText();
            content.setFont(heading ? bold : regular, heading ? 13 : 10);
            content.newLineAtOffset(50, y);
            content.showText(asciiPdfText(text));
            content.endText();
            y -= heading ? 22 : 16;
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - 50;
        }

        private void finish() throws IOException {
            content.close();
        }

        private String asciiPdfText(String text) {
            StringBuilder result = new StringBuilder(text.length());
            text.codePoints().forEach(codePoint -> result.append(
                    codePoint >= 32 && codePoint <= 126 ? (char) codePoint : '?'));
            return result.toString();
        }
    }
}
