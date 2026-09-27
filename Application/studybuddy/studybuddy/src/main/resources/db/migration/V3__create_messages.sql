CREATE TABLE messages (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	sender_id BIGINT NOT NULL,
	receiver_id BIGINT NOT NULL,
	content VARCHAR(1000) NOT NULL,
	sent_at TIMESTAMP NOT NULL,
	FOREIGN KEY (sender_id) REFERENCES users(id),
	FOREIGN KEY (receiver_id) REFERENCES users(id)
);