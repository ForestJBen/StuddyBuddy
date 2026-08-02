create database studybuddy;
use studybuddy;

#Table Creation

create table users (
	id bigint auto_increment primary key,
    first_name varchar(50) not null,
    last_name varchar(50) not null,
    email varchar(50) unique not null,
    password varchar(255) not null,
    programme varchar(100),
    year_level int,
    study_style varchar(500),
    bio text,
    role enum('Student', 'admin')
);

create table courses (
	id bigint auto_increment primary key,
    course_code varchar(20) unique,
    course_name varchar(100)
);

create table user_courses (
	user_id bigint,
    course_id bigint,
    primary key(user_id, course_id),
    foreign key(user_id)
		references users(id),
	foreign key(course_id)
		references courses(id)
);

create table availability (
	id bigint auto_increment primary key,
    user_id bigint,
    day_of_week enum('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'),
    start_time time,
    end_time time,
    foreign key(user_id)
		references users(id)
);

create table study_groups (
	id bigint auto_increment primary key,
    group_name varchar(100),
    description text,
    creator_id bigint,
    foreign key(creator_id)
		references users(id)
);

create table group_members (
	group_id bigint,
    user_id bigint,
    joined_at timestamp default current_timestamp,
    primary key(group_id,user_id),
    foreign key(group_id)
		references study_groups(id),
	foreign key(user_id)
		references users(id)
);

create table partner_requests (
	id bigint auto_increment primary key,
    sender_id bigint,
    receiver_id bigint,
    status enum('Pending', 'Accepted', 'Declined'),
    sent_at timestamp default current_timestamp,
    foreign key(sender_id)
		references users(id),
	foreign key(receiver_id)
		references users(id)
);

create table study_sessions (
	id bigint auto_increment primary key,
    group_id bigint,
    course_id bigint,
    topic varchar(100),
    session_date date,
    session_time time,
    location varchar(255),
    online_link varchar(255),
    foreign key(group_id)
		references study_groups(id),
	foreign key(course_id)
		references courses(id)
);

create table feedback (
	id bigint auto_increment primary key,
    session_id bigint,
    user_id bigint,
    rating int,
    notes text,
    foreign key(session_id)
		references study_sessions(id),
	foreign key(user_id)
		references users(id)
);

create table notifications (
	id bigint auto_increment primary key,
    user_id bigint,
    message text,
    is_read boolean default false,
    created_at timestamp default current_timestamp,
    foreign key(user_id)
		references users(id)
);

create table reports (
	id bigint auto_increment primary key,
    reporter_id bigint,
    reported_user_id bigint,
    reason text,
    status enum('Open', 'Resolved'),
    foreign key(reporter_id)
		references users(id),
	foreign key(reported_user_id)
		references users(id)
);
    

    
    
