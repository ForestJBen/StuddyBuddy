ALTER TABLE group_members
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT; 
    DROP PRIMARY KEY;
    ADD PRIMARY KEY(id);
    ADD CONSTRAINT uk_group_members_group_user
        UNIQUE (group_id, user_id);