CREATE TABLE documents (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    owner_user_id UUID NOT NULL,
    department_id UUID,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_documents_id_institution UNIQUE (id, institution_id),
    CONSTRAINT fk_documents_owner_tenant
        FOREIGN KEY (owner_user_id, institution_id) REFERENCES users (id, institution_id),
    CONSTRAINT fk_documents_department_tenant
        FOREIGN KEY (department_id, institution_id) REFERENCES departments (id, institution_id),
    CONSTRAINT ck_documents_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE INDEX ix_documents_tenant_category ON documents (institution_id, category);
CREATE INDEX ix_documents_tenant_department ON documents (institution_id, department_id);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    institution_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    uploader_user_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_document_versions_number UNIQUE (document_id, version_number),
    CONSTRAINT fk_document_versions_document_tenant
        FOREIGN KEY (document_id, institution_id) REFERENCES documents (id, institution_id),
    CONSTRAINT fk_document_versions_uploader_tenant
        FOREIGN KEY (uploader_user_id, institution_id) REFERENCES users (id, institution_id),
    CONSTRAINT ck_document_versions_number CHECK (version_number > 0),
    CONSTRAINT ck_document_versions_size CHECK (size_bytes > 0 AND size_bytes <= 10485760)
);
CREATE INDEX ix_document_versions_tenant_document ON document_versions (institution_id, document_id, version_number);
