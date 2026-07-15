ALTER TABLE reviews ADD COLUMN IF NOT EXISTS profile_accurate BOOLEAN;

CREATE TABLE IF NOT EXISTS review_profile_mismatches (
    review_id BIGINT NOT NULL,
    field VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (review_id, field),
    CONSTRAINT fk_review_profile_mismatches_review FOREIGN KEY (review_id) REFERENCES reviews (id)
);
