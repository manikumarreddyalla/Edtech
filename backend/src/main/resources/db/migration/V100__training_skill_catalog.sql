CREATE TABLE training_skills (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_training_skills_institution_normalized_name UNIQUE (institution_id, normalized_name),
    CONSTRAINT uq_training_skills_id_institution UNIQUE (id, institution_id),
    CONSTRAINT ck_training_skills_name_nonblank CHECK (length(trim(name)) > 0)
);

CREATE INDEX ix_training_skills_institution_name ON training_skills (institution_id, name);
