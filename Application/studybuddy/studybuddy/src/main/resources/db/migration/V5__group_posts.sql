CREATE TABLE group_posts(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (group_id)
        REFERENCES study_groups(id),
    FOREIGN KEY (author_id)
        REFERENCES users(id)    
);

ALTER TABLE study_groups
    RENAME COLUMN group_name TO name;
ALTER TABLE study_groups
    MODIFY COLUMN name VARCHAR(120) NOT NULL;
ALTER TABLE study_groups
    ADD COLUMN course VARCHAR(20) NOT NULL;
ALTER TABLE study_groups
    MODIFY COLUMN description VARCHAR(2000) NOT NULL;
ALTER TABLE study_groups
    ADD COLUMN meeting_time VARCHAR(100);
ALTER TABLE study_groups
    ADD COLUMN location VARCHAR(200);
ALTER TABLE study_groups
    ADD COLUMN created_at TIMESTAMP NOT NULL;