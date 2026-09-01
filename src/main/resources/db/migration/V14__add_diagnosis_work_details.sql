ALTER TABLE diagnosis_works
  ADD COLUMN diagnosis_time VARCHAR(64) NOT NULL DEFAULT '',
  ADD COLUMN diagnosis_round INT NOT NULL DEFAULT 1,
  ADD COLUMN service_provider_contact VARCHAR(128) NULL;
