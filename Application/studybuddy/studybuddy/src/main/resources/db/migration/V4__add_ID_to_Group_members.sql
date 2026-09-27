ALTER TABLE group_members
    DROP FOREIGN KEY group_members_ibfk_1,
    DROP FOREIGN KEY group_members_ibfk_2;

ALTER TABLE group_members
    DROP PRIMARY KEY;
ALTER TABLE group_members
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY;
ALTER TABLE group_members
    ADD CONSTRAINT uk_group_members_group_user
        UNIQUE (group_id, user_id);

ALTER TABLE group_members
    ADD CONSTRAINT fk_group_members_group
        FOREIGN KEY (group_id)
        REFERENCES study_groups(id)
        ON DELETE CASCADE;
ALTER TABLE group_members
    ADD CONSTRAINT fk_group_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;
