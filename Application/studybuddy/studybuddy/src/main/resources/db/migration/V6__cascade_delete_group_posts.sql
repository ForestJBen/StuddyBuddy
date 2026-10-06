ALTER TABLE group_posts
    DROP FOREIGN KEY group_posts_ibfk_1;

ALTER TABLE group_posts
    ADD CONSTRAINT fk_group_posts_group
        FOREIGN KEY (group_id)
        REFERENCES study_groups(id)
        ON DELETE CASCADE;