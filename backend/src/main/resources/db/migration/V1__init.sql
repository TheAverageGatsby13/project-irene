CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patient_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    age_band VARCHAR(50) NOT NULL,
    language_code VARCHAR(20) NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    region VARCHAR(120) NOT NULL,
    availability_slots TEXT NOT NULL,
    group_sharing_comfort VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE program_templates (
    id BIGSERIAL PRIMARY KEY,
    therapist_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    issue_focus TEXT NOT NULL,
    goal_focus TEXT NOT NULL,
    target_size INT NOT NULL,
    schedule_window TEXT NOT NULL,
    language_code VARCHAR(20) NOT NULL,
    region VARCHAR(120) NOT NULL,
    target_age_band VARCHAR(50) NOT NULL,
    min_group_sharing_comfort VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE intake_submissions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES users(id),
    structured_answers TEXT NOT NULL,
    free_text TEXT NOT NULL,
    goals_text TEXT NOT NULL,
    consent_ai BOOLEAN NOT NULL,
    consent_group BOOLEAN NOT NULL,
    not_emergency_ack BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_features (
    id BIGSERIAL PRIMARY KEY,
    intake_submission_id BIGINT NOT NULL UNIQUE REFERENCES intake_submissions(id),
    issue_tags TEXT NOT NULL,
    goals TEXT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    summary TEXT NOT NULL,
    embedding TEXT NOT NULL,
    normalized_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE match_candidates (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES program_templates(id),
    patient_id BIGINT NOT NULL REFERENCES users(id),
    intake_submission_id BIGINT NOT NULL REFERENCES intake_submissions(id),
    run_id VARCHAR(64) NOT NULL,
    hard_filter_pass BOOLEAN NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_candidates_template_run ON match_candidates(template_id, run_id);

CREATE TABLE therapy_groups (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES program_templates(id),
    therapist_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE group_memberships (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES therapy_groups(id),
    patient_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, patient_id)
);
