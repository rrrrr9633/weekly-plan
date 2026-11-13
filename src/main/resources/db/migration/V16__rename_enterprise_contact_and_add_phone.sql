ALTER TABLE diagnosis_works
  RENAME COLUMN service_provider_contact TO enterprise_contact;

ALTER TABLE diagnosis_works
  ADD COLUMN enterprise_contact_phone VARCHAR(64) NULL;
