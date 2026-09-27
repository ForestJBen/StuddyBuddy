ALTER TABLE group_members
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    ADD CONSTRAINT uk_group_members_group_user
        UNIQUE (group_id, user_id);