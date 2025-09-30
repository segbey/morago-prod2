INSERT INTO categories (name, is_active, created_at)
VALUES
('WORK', TRUE, NOW()),
('MEDICINE', TRUE, NOW()),
('AUTO', TRUE, NOW()),
('SERVICES', TRUE, NOW()),
('BUSINESS', TRUE, NOW()),
('GOVERNMENT', TRUE, NOW()),
('LAW', TRUE, NOW()),
('EMERGENCY', TRUE, NOW())
ON DUPLICATE KEY UPDATE
  is_active = VALUES(is_active);