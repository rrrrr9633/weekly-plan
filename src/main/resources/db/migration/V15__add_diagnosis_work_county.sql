ALTER TABLE diagnosis_works
  ADD COLUMN county VARCHAR(128) NOT NULL DEFAULT '';

UPDATE diagnosis_works
SET county = LEFT(address, 128)
WHERE county = '';
