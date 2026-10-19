package com.collegeops.documents;

import com.collegeops.common.PageResponse;
import com.collegeops.common.RequestIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'IQAC_COORDINATOR', 'FACULTY')")
public class DocumentManagementController {
    private final DocumentManagementService service;

    public DocumentManagementController(DocumentManagementService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document and create its first immutable version")
    public ResponseEntity<DocumentUploadResponse> create(
            JwtAuthenticationToken authentication,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam("file") MultipartFile file) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return ResponseEntity.status(201).body(service.create(identity.institutionId(), identity.userId(),
                title, category, departmentId, file));
    }

    @GetMapping
    @Operation(summary = "List tenant-scoped documents with optional filters")
    public PageResponse<DocumentResponse> list(
            JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID departmentId) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.list(identity.institutionId(), identity.userId(), roles(authentication),
                page, size, q, category, departmentId);
    }

    @PostMapping(path = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new immutable version")
    public DocumentVersionResponse addVersion(
            JwtAuthenticationToken authentication,
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.addVersion(identity.institutionId(), identity.userId(), roles(authentication),
                documentId, file);
    }

    @GetMapping("/{documentId}/versions")
    @Operation(summary = "List version metadata for an accessible document")
    public PageResponse<DocumentVersionResponse> versions(
            JwtAuthenticationToken authentication,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        return service.versions(identity.institutionId(), identity.userId(), roles(authentication),
                documentId, page, size);
    }

    @GetMapping("/{documentId}/versions/{versionNumber}/content")
    @Operation(summary = "Download a private document version")
    public ResponseEntity<ByteArrayResource> download(
            JwtAuthenticationToken authentication,
            @PathVariable UUID documentId,
            @PathVariable int versionNumber) {
        RequestIdentity identity = RequestIdentity.from(authentication);
        DocumentDownload download = service.download(identity.institutionId(), identity.userId(),
                roles(authentication), documentId, versionNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentLength(download.content().length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(download.content()));
    }

    private List<String> roles(JwtAuthenticationToken authentication) {
        List<String> roles = authentication.getToken().getClaimAsStringList("roles");
        return roles == null ? List.of() : roles;
    }
}
