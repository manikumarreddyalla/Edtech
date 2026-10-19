ALTER TABLE document_versions
    ADD CONSTRAINT uq_document_versions_document_tenant_number
    UNIQUE (document_id, institution_id, version_number);

CREATE TABLE accreditation_frameworks (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    name VARCHAR(200) NOT NULL,
    version_label VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_frameworks_tenant_name_version UNIQUE (institution_id, name, version_label),
    CONSTRAINT uq_frameworks_id_tenant UNIQUE (id, institution_id),
    CONSTRAINT ck_frameworks_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE TABLE accreditation_criteria (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    framework_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_criteria_framework_code UNIQUE (framework_id, code),
    CONSTRAINT uq_criteria_id_tenant UNIQUE (id, institution_id),
    CONSTRAINT fk_criteria_framework_tenant
        FOREIGN KEY (framework_id, institution_id)
        REFERENCES accreditation_frameworks (id, institution_id)
);
CREATE INDEX ix_criteria_tenant_framework ON accreditation_criteria (institution_id, framework_id);

CREATE TABLE evidence_assignments (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    criterion_id UUID NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    department_id UUID,
    owner_user_id UUID NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_evidence_assignments_id_tenant UNIQUE (id, institution_id),
    CONSTRAINT fk_evidence_criterion_tenant
        FOREIGN KEY (criterion_id, institution_id) REFERENCES accreditation_criteria (id, institution_id),
    CONSTRAINT fk_evidence_department_tenant
        FOREIGN KEY (department_id, institution_id) REFERENCES departments (id, institution_id),
    CONSTRAINT fk_evidence_owner_tenant
        FOREIGN KEY (owner_user_id, institution_id) REFERENCES users (id, institution_id),
    CONSTRAINT ck_evidence_status CHECK (
        status IN ('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'NEEDS_REVISION', 'VERIFIED')
    )
);
CREATE INDEX ix_evidence_tenant_year_status ON evidence_assignments (institution_id, academic_year, status);
CREATE INDEX ix_evidence_tenant_owner ON evidence_assignments (institution_id, owner_user_id);
CREATE INDEX ix_evidence_tenant_department ON evidence_assignments (institution_id, department_id);

CREATE TABLE evidence_submissions (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    evidence_assignment_id UUID NOT NULL,
    submission_number INTEGER NOT NULL,
    submitter_user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_evidence_submissions_number UNIQUE (evidence_assignment_id, submission_number),
    CONSTRAINT uq_evidence_submissions_id_tenant UNIQUE (id, institution_id),
    CONSTRAINT fk_evidence_submissions_assignment_tenant
        FOREIGN KEY (evidence_assignment_id, institution_id)
        REFERENCES evidence_assignments (id, institution_id),
    CONSTRAINT fk_evidence_submissions_submitter_tenant
        FOREIGN KEY (submitter_user_id, institution_id) REFERENCES users (id, institution_id),
    CONSTRAINT ck_evidence_submission_number CHECK (submission_number > 0),
    CONSTRAINT ck_evidence_submission_status CHECK (status IN ('SUBMITTED', 'NEEDS_REVISION', 'VERIFIED'))
);
CREATE INDEX ix_evidence_submissions_tenant_assignment
    ON evidence_submissions (institution_id, evidence_assignment_id, submission_number);

CREATE TABLE evidence_attachments (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    submission_id UUID NOT NULL,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    CONSTRAINT uq_evidence_attachment_version UNIQUE (submission_id, document_id, version_number),
    CONSTRAINT fk_evidence_attachments_submission_tenant
        FOREIGN KEY (submission_id, institution_id)
        REFERENCES evidence_submissions (id, institution_id),
    CONSTRAINT fk_evidence_attachments_document_version_tenant
        FOREIGN KEY (document_id, institution_id, version_number)
        REFERENCES document_versions (document_id, institution_id, version_number)
);

CREATE TABLE evidence_reviews (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    submission_id UUID NOT NULL,
    reviewer_user_id UUID NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comment VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_evidence_reviews_submission_tenant
        FOREIGN KEY (submission_id, institution_id)
        REFERENCES evidence_submissions (id, institution_id),
    CONSTRAINT fk_evidence_reviews_reviewer_tenant
        FOREIGN KEY (reviewer_user_id, institution_id) REFERENCES users (id, institution_id),
    CONSTRAINT ck_evidence_review_decision CHECK (decision IN ('NEEDS_REVISION', 'VERIFIED'))
);
CREATE INDEX ix_evidence_reviews_tenant_submission ON evidence_reviews (institution_id, submission_id, created_at);
