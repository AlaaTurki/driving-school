CREATE TABLE candidate_profiles
(
    user_id    UUID         NOT NULL,
    full_name  VARCHAR(120) NOT NULL,
    phone      VARCHAR(30)  NOT NULL DEFAULT '',
    CONSTRAINT pk_candidate_profiles PRIMARY KEY (user_id),
    CONSTRAINT fk_candidate_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

INSERT INTO candidate_profiles (user_id, full_name)
SELECT users.id, left(split_part(users.email, '@', 1), 120)
FROM users
JOIN user_roles ON user_roles.user_id = users.id
WHERE user_roles.role = 'CANDIDATE';
