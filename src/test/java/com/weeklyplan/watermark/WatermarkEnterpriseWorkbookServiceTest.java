package com.weeklyplan.watermark;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WatermarkEnterpriseWorkbookServiceTest {
  private final WatermarkEnterpriseWorkbookService importer = new WatermarkEnterpriseWorkbookService();

  @Test
  void keepsNameCityAndCountyAndDeduplicates() throws Exception {
    byte[] workbook = workbook(sheet -> {
      writeRow(sheet, 0, "企业名称", "统一社会信用代码", "行业", "市", "区县", "联系人");
      writeRow(sheet, 1, "辽宁福顺堂药业有限公司", "9121", "医药制造业", "铁岭市", "西丰县", "王春凤");
      writeRow(sheet, 2, "鞍山制药有限公司", "9122", "医药制造业", "鞍山市", "铁西区", "张军");
      writeRow(sheet, 3, "辽宁阳光健康科技有限公司", "9123", "医药制造业", "沈阳市", "铁西区", "宋明");
      writeRow(sheet, 4, "", "9124", "医药制造业", "沈阳市", "和平区", "空名称");
      writeRow(sheet, 5, "鞍山制药有限公司", "9122", "医药制造业", "鞍山市", "铁西区", "重复");
    });

    List<ImportedWatermarkEnterprise> rows = importer.parse(file("企业.xlsx", workbook));

    assertEquals(List.of(
        new ImportedWatermarkEnterprise("辽宁福顺堂药业有限公司", "铁岭市", "西丰县"),
        new ImportedWatermarkEnterprise("鞍山制药有限公司", "鞍山市", "铁西区"),
        new ImportedWatermarkEnterprise("辽宁阳光健康科技有限公司", "沈阳市", "铁西区")
    ), rows);
  }

  @Test
  void acceptsAltCountyHeader() throws Exception {
    byte[] workbook = workbook(sheet -> {
      writeRow(sheet, 0, "企业", "市", "区（县）");
      writeRow(sheet, 1, "旧企业", "锦州市", "凌河区");
    });

    assertEquals(
        List.of(new ImportedWatermarkEnterprise("旧企业", "锦州市", "凌河区")),
        importer.parse(file("旧表.xlsx", workbook))
    );
  }

  private byte[] workbook(SheetWriter writer) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      writer.write(workbook.createSheet("企业"));
      workbook.write(output);
      return output.toByteArray();
    }
  }

  private void writeRow(org.apache.poi.ss.usermodel.Sheet sheet, int index, String... values) {
    var row = sheet.createRow(index);
    for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(values[column]);
  }

  private MultipartFile file(String filename, byte[] content) throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(filename);
    when(file.isEmpty()).thenReturn(false);
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    return file;
  }

  @FunctionalInterface
  private interface SheetWriter {
    void write(org.apache.poi.ss.usermodel.Sheet sheet);
  }
}
