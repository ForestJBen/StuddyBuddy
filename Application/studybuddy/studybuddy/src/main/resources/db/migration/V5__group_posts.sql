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
    CHANGE COLUMN group_name name VARCHAR(120) NOT NULL,
    ADD COLUMN course VARCHAR(20) NOT NULL,
    MODIFY COLUMN description VARCHAR(2000) NOT NULL,
    ADD COLUMN meeting_time VARCHAR(100),
    ADD COLUMN location VARCHAR(200),
    ADD COLUMN created_at TIMESTAMP NOT NULL;