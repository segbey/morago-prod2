INSERT INTO users (phone_number, password, is_active)
VALUES ('01012345671',
        '$2a$10$7eqJtq98hPqEX7fNZaFWoOhi5y1Q9rW6j5x0VfLPMo5kFQvG6XK5e',
        true)
    ON DUPLICATE KEY UPDATE phone_number = phone_number;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.phone_number = '01012345671'
    ON DUPLICATE KEY UPDATE role_id = role_id;
