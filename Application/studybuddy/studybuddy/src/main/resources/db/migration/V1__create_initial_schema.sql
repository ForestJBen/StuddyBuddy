CREATE TABLE users(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE TABLE courses (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL
);
CREATE TABLE user_courses(
	user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY(user_id,course_id),
    FOREIGN KEY(user_id)
		REFERENCES users(id),
	FOREIGN KEY(course_id)
		REFERENCES courses(id)
        ON DELETE CASCADE
);
CREATE TABLE availability(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    day_of_week ENUM('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'),
    start_time TIME,
    end_time TIME,
    FOREIGN KEY (user_id)
		REFERENCES users(id)
        ON DELETE CASCADE
);
CREATE TABLE study_groups(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(100),
    description TEXT,
    creator_id BIGINT NOT NULL,
    FOREIGN KEY (creator_id)
		REFERENCES users(id)
);
CREATE TABLE group_members(
	group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id)
		REFERENCES study_groups(id)
        ON DELETE CASCADE,
	FOREIGN KEY (user_id)
		REFERENCES users(id)
        ON DELETE CASCADE
);
CREATE TABLE partner_requests(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    status ENUM('Pending', 'Accepted', 'Declined'),
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id)
		REFERENCES users(id),
	FOREIGN KEY (receiver_id)
		REFERENCES users(id)
);
CREATE TABLE study_sessions(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    topic VARCHAR(100),
    session_date DATE,
    session_time TIME,
    location VARCHAR(255),
    online_link VARCHAR(255),
    FOREIGN KEY (group_id)
		REFERENCES study_groups(id),
	FOREIGN KEY (course_id)
		REFERENCES courses(id)
);
CREATE TABLE feedback(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT,
    notes TEXT,
    FOREIGN KEY (session_id)
		REFERENCES study_sessions(id),
	FOREIGN KEY (user_id)
		REFERENCES users(id)
);
CREATE TABLE notifications(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)
		REFERENCES users(id)
        ON DELETE CASCADE
);
CREATE TABLE reports (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT NOT NULL,
    reason TEXT,
    status ENUM('Open', 'Resolved'),
    FOREIGN KEY (reporter_id)
		REFERENCES users(id),
	FOREIGN KEY (reported_user_id)
		REFERENCES users(id)
); 