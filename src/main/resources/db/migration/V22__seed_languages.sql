INSERT INTO languages (name, created_at)
VALUES
('English', NOW()),
('Russian', NOW()),
('Korean', NOW()),
('Chinese', NOW()),
('Japanese', NOW()),
('Spanish', NOW()),
('French', NOW()),
('German', NOW()),
('Italian', NOW()),
('Arabic', NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name);