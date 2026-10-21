CREATE TABLE training_job_roles (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_training_job_roles_tenant_name UNIQUE(institution_id, name),
    CONSTRAINT uq_training_job_roles_id_tenant UNIQUE(id, institution_id)
);

CREATE TABLE training_role_skill_requirements (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    job_role_id UUID NOT NULL,
    skill_id UUID NOT NULL,
    target_level INTEGER NOT NULL CHECK(target_level BETWEEN 1 AND 5),
    CONSTRAINT fk_training_role_requirements_role FOREIGN KEY(job_role_id, institution_id) REFERENCES training_job_roles(id, institution_id),
    CONSTRAINT fk_training_role_requirements_skill FOREIGN KEY(skill_id, institution_id) REFERENCES training_skills(id, institution_id),
    CONSTRAINT uq_training_role_requirement UNIQUE(job_role_id, skill_id)
);

CREATE TABLE training_pathways (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    name VARCHAR(180) NOT NULL,
    version INTEGER NOT NULL CHECK(version > 0),
    description VARCHAR(3000),
    status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    synthetic BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_training_pathway_version UNIQUE(institution_id, name, version),
    CONSTRAINT uq_training_pathway_id_tenant UNIQUE(id, institution_id)
);

CREATE TABLE training_pathway_modules (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    pathway_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL CHECK(sequence_number > 0),
    title VARCHAR(180) NOT NULL,
    description VARCHAR(2000),
    estimated_hours INTEGER NOT NULL CHECK(estimated_hours > 0),
    CONSTRAINT fk_training_module_pathway FOREIGN KEY(pathway_id, institution_id) REFERENCES training_pathways(id, institution_id),
    CONSTRAINT uq_training_module_sequence UNIQUE(pathway_id, sequence_number),
    CONSTRAINT uq_training_module_id_tenant UNIQUE(id, institution_id)
);

CREATE TABLE training_cohorts (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    pathway_id UUID NOT NULL,
    name VARCHAR(180) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK(status IN ('PLANNED','ACTIVE','COMPLETED','CANCELLED')),
    start_date DATE NOT NULL,
    end_date DATE,
    trainer_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_training_cohort_pathway FOREIGN KEY(pathway_id, institution_id) REFERENCES training_pathways(id, institution_id),
    CONSTRAINT uq_training_cohort_id_tenant UNIQUE(id, institution_id),
    CONSTRAINT ck_training_cohort_dates CHECK(end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE training_enrollments (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    cohort_id UUID NOT NULL,
    student_id UUID NOT NULL REFERENCES students(id),
    status VARCHAR(20) NOT NULL CHECK(status IN ('ENROLLED','COMPLETED','WITHDRAWN')),
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_training_enrollment_cohort FOREIGN KEY(cohort_id, institution_id) REFERENCES training_cohorts(id, institution_id),
    CONSTRAINT uq_training_enrollment UNIQUE(cohort_id, student_id),
    CONSTRAINT uq_training_enrollment_id_tenant UNIQUE(id, institution_id)
);

CREATE TABLE training_assignments (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    cohort_id UUID NOT NULL,
    module_id UUID,
    title VARCHAR(180) NOT NULL,
    instructions VARCHAR(5000) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_training_assignment_cohort FOREIGN KEY(cohort_id, institution_id) REFERENCES training_cohorts(id, institution_id),
    CONSTRAINT fk_training_assignment_module FOREIGN KEY(module_id, institution_id) REFERENCES training_pathway_modules(id, institution_id),
    CONSTRAINT uq_training_assignment_id_tenant UNIQUE(id, institution_id)
);

CREATE TABLE training_assignment_rubric_items (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    assignment_id UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    max_points INTEGER NOT NULL CHECK(max_points > 0),
    sequence_number INTEGER NOT NULL CHECK(sequence_number > 0),
    CONSTRAINT fk_training_rubric_assignment FOREIGN KEY(assignment_id, institution_id) REFERENCES training_assignments(id, institution_id),
    CONSTRAINT uq_training_rubric_sequence UNIQUE(assignment_id, sequence_number)
    ,CONSTRAINT uq_training_rubric_id_tenant UNIQUE(id, institution_id)
);

CREATE TABLE training_assessment_attempts (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    assignment_id UUID NOT NULL,
    student_id UUID NOT NULL REFERENCES students(id),
    attempt_number INTEGER NOT NULL CHECK(attempt_number > 0),
    status VARCHAR(20) NOT NULL CHECK(status IN ('SUBMITTED','GRADED')),
    submission_text VARCHAR(10000) NOT NULL,
    score INTEGER,
    feedback VARCHAR(3000),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    graded_by UUID,
    graded_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_training_attempt_assignment FOREIGN KEY(assignment_id, institution_id) REFERENCES training_assignments(id, institution_id),
    CONSTRAINT uq_training_attempt_number UNIQUE(assignment_id, student_id, attempt_number),
    CONSTRAINT uq_training_attempt_id_tenant UNIQUE(id, institution_id)
);

CREATE TABLE training_attempt_rubric_scores (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    attempt_id UUID NOT NULL,
    rubric_item_id UUID NOT NULL,
    points_awarded INTEGER NOT NULL CHECK(points_awarded >= 0),
    CONSTRAINT fk_training_score_attempt FOREIGN KEY(attempt_id, institution_id) REFERENCES training_assessment_attempts(id, institution_id),
    CONSTRAINT fk_training_score_rubric FOREIGN KEY(rubric_item_id, institution_id) REFERENCES training_assignment_rubric_items(id, institution_id),
    CONSTRAINT uq_training_attempt_rubric UNIQUE(attempt_id, rubric_item_id)
);

CREATE TABLE training_feedback (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    cohort_id UUID NOT NULL,
    student_id UUID NOT NULL REFERENCES students(id),
    trainer_user_id UUID NOT NULL,
    message VARCHAR(3000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_training_feedback_cohort FOREIGN KEY(cohort_id, institution_id) REFERENCES training_cohorts(id, institution_id)
);

CREATE INDEX ix_training_cohorts_tenant_status ON training_cohorts(institution_id, status);
CREATE INDEX ix_training_enrollments_student ON training_enrollments(institution_id, student_id);
CREATE INDEX ix_training_assignments_cohort ON training_assignments(institution_id, cohort_id);
CREATE INDEX ix_training_attempts_student ON training_assessment_attempts(institution_id, student_id);
