package com.collegeops.documents;

import com.collegeops.common.AuditEvent;
import com.collegeops.common.AuditEventRepository;
import com.collegeops.common.InvalidRequestException;
import com.collegeops.common.PageResponse;
import com.collegeops.common.ResourceNotFoundException;
import com.collegeops.institution.Department;
import com.collegeops.institution.DepartmentRepository;
import com.collegeops.institution.Institution;
import com.collegeops.institution.InstitutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentManagementService {
    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);
    private static final long DATABASE_FILE_SIZE_LIMIT = 10_485_760L;
    private static final String ADMIN = "INSTITUTION_ADMIN";
    private static final String REVIEWER = "IQAC_COORDINATOR";
    private static final String FACULTY = "FACULTY";

    private final InstitutionRepository institutions;
    private final DepartmentRepository departments;
    private final DocumentRecordRepository documents;
    private final DocumentVersionRepository versions;
    private final AuditEventRepository auditEvents;
    private final DocumentStorage storage;
    private final long maxFileBytes;

    public DocumentManagementService(InstitutionRepository institutions, DepartmentRepository departments,
                                     DocumentRecordRepository documents, DocumentVersionRepository versions,
                                     AuditEventRepository auditEvents, DocumentStorage storage,
                                     @Value("${app.documents.max-file-bytes}") long maxFileBytes) {
        this.institutions = institutions;
        this.departments = departments;
        this.documents = documents;
        this.versions = versions;
        this.auditEvents = auditEvents;
        this.storage = storage;
        if (maxFileBytes < 1 || maxFileBytes > DATABASE_FILE_SIZE_LIMIT) {
            throw new IllegalArgumentException("app.documents.max-file-bytes must be between 1 and 10485760.");
        }
        this.maxFileBytes = maxFileBytes;
    }

    @Transactional
    public DocumentUploadResponse create(UUID institutionId, UUID actorId, String title, String category,
                                         UUID departmentId, MultipartFile file) {
        String normalizedTitle = requiredText(title, 200, "title");
        String normalizedCategory = requiredText(category, 50, "category").toUpperCase(Locale.ROOT);
        Institution institution = institutions.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution"));
        Department department = departmentId == null ? null : departments
                .findByIdAndInstitution_Id(departmentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Department"));
        ValidatedFile upload = validateFile(file);

        DocumentRecord document = documents.save(new DocumentRecord(institution, actorId, department,
                normalizedTitle, normalizedCategory));
        String storageKey = storage.store(upload.content(), upload.extension());
        cleanUpOnRollback(storageKey);
        DocumentVersion version = versions.save(new DocumentVersion(document, 1, actorId,
                upload.filename(), upload.contentType(), upload.content().length,
                sha256(upload.content()), storageKey));
        auditEvents.save(new AuditEvent(institutionId, actorId, "DOCUMENT_UPLOADED", "DOCUMENT_VERSION",
                version.getId(), "Document version 1 uploaded."));
        return new DocumentUploadResponse(DocumentResponse.from(document), DocumentVersionResponse.from(version));
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> list(UUID institutionId, UUID userId, List<String> roles,
                                               int page, int size, String query, String category,
                                               UUID departmentId) {
        validatePage(page, size);
        UUID ownerFilter = canReview(roles) ? null : userId;
        if (departmentId != null) {
            departments.findByIdAndInstitution_Id(departmentId, institutionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department"));
        }
        String normalizedQuery = optionalText(query, 200, "q");
        String normalizedCategory = optionalText(category, 50, "category");
        if (normalizedCategory != null) {
            normalizedCategory = normalizedCategory.toUpperCase(Locale.ROOT);
        }
        return PageResponse.from(documents.search(institutionId, ownerFilter, normalizedQuery,
                        normalizedCategory, departmentId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .map(DocumentResponse::from));
    }

    @Transactional
    public DocumentVersionResponse addVersion(UUID institutionId, UUID actorId, List<String> roles,
                                              UUID documentId, MultipartFile file) {
        DocumentRecord document = documents.findForUpdateByIdAndInstitutionId(documentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Document"));
        requireDocumentAccess(document, actorId, roles);
        ValidatedFile upload = validateFile(file);
        int versionNumber = versions
                .findTopByDocument_IdAndInstitutionIdOrderByVersionNumberDesc(documentId, institutionId)
                .map(version -> version.getVersionNumber() + 1)
                .orElseThrow(() -> new ResourceNotFoundException("Document version"));
        String storageKey = storage.store(upload.content(), upload.extension());
        cleanUpOnRollback(storageKey);
        DocumentVersion version = versions.save(new DocumentVersion(document, versionNumber, actorId,
                upload.filename(), upload.contentType(), upload.content().length,
                sha256(upload.content()), storageKey));
        auditEvents.save(new AuditEvent(institutionId, actorId, "DOCUMENT_VERSION_UPLOADED",
                "DOCUMENT_VERSION", version.getId(), "Document version " + versionNumber + " uploaded."));
        return DocumentVersionResponse.from(version);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentVersionResponse> versions(UUID institutionId, UUID userId, List<String> roles,
                                                          UUID documentId, int page, int size) {
        validatePage(page, size);
        DocumentRecord document = document(institutionId, documentId);
        requireDocumentAccess(document, userId, roles);
        return PageResponse.from(versions.findAllByDocument_IdAndInstitutionId(documentId, institutionId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "versionNumber")))
                .map(DocumentVersionResponse::from));
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(UUID institutionId, UUID userId, List<String> roles,
                                     UUID documentId, int versionNumber) {
        DocumentRecord document = document(institutionId, documentId);
        requireDocumentAccess(document, userId, roles);
        DocumentVersion version = versions
                .findByDocument_IdAndInstitutionIdAndVersionNumber(documentId, institutionId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Document version"));
        return new DocumentDownload(version.getContentType(), version.getOriginalFilename(),
                storage.read(version.getStorageKey()));
    }

    private DocumentRecord document(UUID institutionId, UUID documentId) {
        return documents.findByIdAndInstitution_Id(documentId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Document"));
    }

    private void requireDocumentAccess(DocumentRecord document, UUID userId, List<String> roles) {
        if (!canReview(roles) && !(roles.contains(FACULTY) && document.getOwnerUserId().equals(userId))) {
            throw new ResourceNotFoundException("Document");
        }
    }

    private boolean canReview(List<String> roles) {
        return roles.contains(ADMIN) || roles.contains(REVIEWER);
    }

    private ValidatedFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("A non-empty file is required.");
        }
        if (file.getSize() > maxFileBytes) {
            throw new InvalidRequestException("The file exceeds the configured size limit.");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read uploaded document.", exception);
        }

        String contentType = file.getContentType() == null
                ? "" : file.getContentType().split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        String extension;
        if ("application/pdf".equals(contentType) && startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
            extension = ".pdf";
        } else if ("image/png".equals(contentType) && startsWith(content,
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) {
            extension = ".png";
        } else if ("image/jpeg".equals(contentType) && startsWith(content,
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff})) {
            extension = ".jpg";
        } else if ("text/plain".equals(contentType) && isUtf8Text(content)) {
            extension = ".txt";
        } else {
            throw new InvalidRequestException("Only valid PDF, JPEG, PNG and UTF-8 plain text files are accepted.");
        }
        return new ValidatedFile(content, extension, contentType, safeFilename(file.getOriginalFilename()));
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isUtf8Text(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private String safeFilename(String originalFilename) {
        String filename = originalFilename == null ? "upload" : originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .strip();
        if (filename.isBlank()) {
            filename = "upload";
        }
        return filename.length() > 255 ? filename.substring(0, 255) : filename;
    }

    private String requiredText(String value, int maxLength, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.length() > maxLength || containsControlCharacter(normalized)) {
            throw new InvalidRequestException("The " + field + " field is invalid.");
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requiredText(value, maxLength, field);
    }

    private boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable in this runtime.", exception);
        }
    }

    private void cleanUpOnRollback(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storage.delete(storageKey);
                    } catch (RuntimeException exception) {
                        log.error("Unable to clean up document content after transaction rollback.", exception);
                    }
                }
            }
        });
    }

    private record ValidatedFile(byte[] content, String extension, String contentType, String filename) {
    }
}
