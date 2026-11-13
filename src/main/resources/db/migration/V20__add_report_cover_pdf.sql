INSERT INTO feature_modules (code, name, description) VALUES
  ('REPORT_COVER_PDF', '报告转PDF', '用盖章封面图替换 Word 第一页并导出 PDF')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description);
