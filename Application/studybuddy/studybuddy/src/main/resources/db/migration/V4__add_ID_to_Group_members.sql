ALTER TABLE group_members
    DROP PRIMARY KEY;
ALTER TABLE group_members
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE group_members
    ADD CONSTRAINT pk_group_members PRIMARY KEY (id);
ALTER TABLE group_members
    ADD CONSTRAINT uk_group_members_group_user
        UNIQUE (group_id, user_id);