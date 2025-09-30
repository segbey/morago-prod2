ALTER TABLE password_resets
  ADD COLUMN code_verified TINYINT(1) NOT NULL DEFAULT 0 AFTER used,
  ADD COLUMN verified_at   DATETIME NULL AFTER code_verified;

CREATE INDEX idx_password_resets_phone ON password_resets (phone);
CREATE INDEX idx_password_resets_token ON password_resets (token);

CREATE INDEX idx_password_resets_verify2
  ON password_resets (phone, reset_code, used, code_verified, expires_at);

ALTER TABLE password_resets MODIFY COLUMN used TINYINT(1) NOT NULL DEFAULT 0;