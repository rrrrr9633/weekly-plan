INSERT INTO feature_modules (code, name, description) VALUES
  ('PHOTO_WATERMARK', '照片加水印', '为企业打卡照片叠加时间与地点水印')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description);

CREATE TABLE watermark_enterprises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  city VARCHAR(64) NOT NULL DEFAULT '',
  county VARCHAR(64) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (company_id, name, city, county),
  INDEX idx_watermark_enterprises_company_place (company_id, city, county),
  CONSTRAINT fk_watermark_enterprises_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);
