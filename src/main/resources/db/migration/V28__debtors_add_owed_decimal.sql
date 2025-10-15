ALTER TABLE debtors
  ADD COLUMN owed_decimal DECIMAL(19,2) NOT NULL DEFAULT 0;

ALTER TABLE debtors
  MODIFY account_holder VARCHAR(200) NULL,
  MODIFY name_of_bank   VARCHAR(200) NULL;

CREATE INDEX idx_debtors_user_paid ON debtors(user_id, is_paid);
