INSERT INTO users (phone_number, password, is_active, created_at)
VALUES ('01098765671',
        '$2a$10$a9MHfzsaZRQKAvWH37cBe.O7qRqSAEKIG3dGMCUVFfGrC7o13ci1.',
        true,
        NOW(6))
    ON DUPLICATE KEY UPDATE phone_number = phone_number;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.role_name = 'ROLE_ADMIN'
WHERE u.phone_number = '01098765671'
    ON DUPLICATE KEY UPDATE role_id = role_id;
