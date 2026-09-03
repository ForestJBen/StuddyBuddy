CREATE TABLE profiles(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    programme VARCHAR(100) NOT NULL,
    year_level INT NOT NULL,
    goals VARCHAR(500),
    study_style VARCHAR(100),
    availability VARCHAR(500),
    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE profile_courses(
    profile_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    CONSTRAINT uk_profile_courses_profile_course
        UNIQUE (profile_id, course_id),
    FOREIGN KEY (profile_id)
        REFERENCES profiles(id)
        ON DELETE CASCADE,
    FOREIGN KEY (course_id)
        REFERENCES courses(id)
);