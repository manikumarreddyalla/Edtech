CREATE TABLE institutions (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_institutions_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_users_institution_email UNIQUE (institution_id, email),
    CONSTRAINT uq_users_id_institution UNIQUE (id, institution_id),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX ix_users_institution_id ON users (institution_id);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_value CHECK (
        role IN ('INSTITUTION_ADMIN', 'IQAC_COORDINATOR', 'FACULTY', 'TRAINER', 'PLACEMENT_OFFICER', 'STUDENT')
    )
);

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    name VARCHAR(180) NOT NULL,
    code VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_departments_institution_code UNIQUE (institution_id, code),
    CONSTRAINT uq_departments_id_institution UNIQUE (id, institution_id),
    CONSTRAINT ck_departments_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE academic_programs (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    department_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_programs_institution_code UNIQUE (institution_id, code),
    CONSTRAINT uq_programs_id_institution UNIQUE (id, institution_id),
    CONSTRAINT fk_programs_department_tenant
        FOREIGN KEY (department_id, institution_id) REFERENCES departments (id, institution_id),
    CONSTRAINT ck_programs_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE students (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    user_id UUID,
    department_id UUID NOT NULL,
    academic_program_id UUID NOT NULL,
    student_number VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_students_institution_number UNIQUE (institution_id, student_number),
    CONSTRAINT uq_students_institution_user UNIQUE (institution_id, user_id),
    CONSTRAINT uq_students_id_institution UNIQUE (id, institution_id),
    CONSTRAINT fk_students_user_tenant
        FOREIGN KEY (user_id, institution_id) REFERENCES users (id, institution_id),
    CONSTRAINT fk_students_department_tenant
        FOREIGN KEY (department_id, institution_id) REFERENCES departments (id, institution_id),
    CONSTRAINT fk_students_program_tenant
        FOREIGN KEY (academic_program_id, institution_id) REFERENCES academic_programs (id, institution_id),
    CONSTRAINT ck_students_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX ix_students_institution_department ON students (institution_id, department_id);
CREATE INDEX ix_students_institution_program ON students (institution_id, academic_program_id);

CREATE TABLE faculty_profiles (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    user_id UUID NOT NULL,
    department_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_faculty_institution_user UNIQUE (institution_id, user_id),
    CONSTRAINT fk_faculty_user_tenant
        FOREIGN KEY (user_id, institution_id) REFERENCES users (id, institution_id),
    CONSTRAINT fk_faculty_department_tenant
        FOREIGN KEY (department_id, institution_id) REFERENCES departments (id, institution_id)
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    actor_user_id UUID REFERENCES users (id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id UUID NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    summary VARCHAR(1000)
);
CREATE INDEX ix_audit_events_tenant_time ON audit_events (institution_id, occurred_at);
