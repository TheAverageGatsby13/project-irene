ALTER TABLE intake_submissions ADD COLUMN summary_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE intake_submissions ADD COLUMN assigned_therapist_id BIGINT REFERENCES users(id);
ALTER TABLE intake_submissions ADD COLUMN program_id BIGINT REFERENCES program_templates(id);
ALTER TABLE intake_submissions ADD COLUMN review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE intake_submissions ADD COLUMN review_note TEXT;
ALTER TABLE intake_submissions ADD COLUMN budget_minor INT NOT NULL DEFAULT 100000;
ALTER TABLE group_memberships ADD COLUMN intake_id BIGINT REFERENCES intake_submissions(id);
ALTER TABLE therapy_groups ADD COLUMN starts_at TIMESTAMP;
ALTER TABLE therapy_groups ADD COLUMN duration_minutes INT NOT NULL DEFAULT 60;
ALTER TABLE therapy_groups ADD COLUMN price_minor INT;
ALTER TABLE therapy_groups ADD COLUMN meeting_url VARCHAR(400);
ALTER TABLE therapy_groups ADD COLUMN preparation_brief TEXT;
CREATE TABLE therapist_credentials (
 user_id BIGINT PRIMARY KEY REFERENCES users(id),
 display_name VARCHAR(100) NOT NULL,
 qualification VARCHAR(300) NOT NULL,
 registration_reference VARCHAR(200) NOT NULL,
 region VARCHAR(80) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 review_note VARCHAR(500),
 reviewed_by BIGINT REFERENCES users(id)
);
CREATE TABLE session_feedback (
 id BIGSERIAL PRIMARY KEY,
 group_id BIGINT NOT NULL REFERENCES therapy_groups(id),
 patient_id BIGINT NOT NULL REFERENCES users(id),
 rating INT NOT NULL,
 comment TEXT NOT NULL,
 UNIQUE(group_id, patient_id)
);
CREATE TABLE activity_events (
 id BIGSERIAL PRIMARY KEY,
 actor_id BIGINT REFERENCES users(id),
 event_type VARCHAR(80) NOT NULL,
 resource_id BIGINT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_intake_review ON intake_submissions(assigned_therapist_id, review_status);
ALTER TABLE program_templates ADD COLUMN timezone VARCHAR(80) NOT NULL DEFAULT 'Asia/Kolkata';
