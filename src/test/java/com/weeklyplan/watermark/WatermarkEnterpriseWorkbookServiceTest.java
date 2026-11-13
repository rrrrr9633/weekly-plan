package com.weeklyplan.watermark;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

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

    assertEquals(List.of(
        new ImportedWatermarkEnterprise("辽宁福顺堂药业有限公司", "铁岭市", "西丰县"),
        new ImportedWatermarkEnterprise("鞍山制药有限公司", "鞍山市", "铁西区"),
        new ImportedWatermarkEnterprise("辽宁阳光健康科技有限公司", "沈阳市", "铁西区")
    ), importer.parse(file("企业.xlsx", workbook)));
  }

  @Test
  void infersHeaderlessExportLikeTheScreenshot() throws Exception {
    byte[] workbook = workbook(sheet -> {
      writeRow(sheet, 0, "辽宁祥兴药业有限公司", "*256*", "医药制造业", "本溪市", "桓仁满族自治县", "仇仁垒");
      writeRow(sheet, 1, "辽宁福顺堂药业有限公司", "9121", "医药制造业", "铁岭市", "西丰县", "王春凤");
      writeRow(sheet, 2, "鞍山制药有限公司", "9122", "医药制造业", "鞍山市", "铁西区", "张军");
      writeRow(sheet, 3, "辽宁阳光健康科技有限公司", "9123", "医药制造业", "沈阳市", "铁西区", "宋明");
    });

    assertEquals(List.of(
        new ImportedWatermarkEnterprise("辽宁祥兴药业有限公司", "本溪市", "桓仁满族自治县"),
        new ImportedWatermarkEnterprise("辽宁福顺堂药业有限公司", "铁岭市", "西丰县"),
        new ImportedWatermarkEnterprise("鞍山制药有限公司", "鞍山市", "铁西区"),
        new ImportedWatermarkEnterprise("辽宁阳光健康科技有限公司", "沈阳市", "铁西区")
    ), importer.parse(file("无表头.xlsx", workbook)));
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

  @Test
  void readsVisitTimesAndIgnoresStaffColumns() throws Exception {
    byte[] workbook = workbook(sheet -> {
      writeRow(sheet, 0, "姓名", "企业名称", "市", "区(县)", "诊断人", "第一次诊断时间", "照片内人员", "第二次诊断时间", "照片内人员", "第三次诊断时间", "报告编制日期");
      writeRow(sheet, 1, "李光宇", "沈阳沈大内窥镜有限公司", "沈阳", "大东区", "李光宇", "2026/5/13 13:56", "甲", "2026/5/22 13:32", "乙", "2026/6/12 9:00", "6/1");
      writeRow(sheet, 2, "李光宇", "潮州安宫药业有限公司", "潮州", "太和区", "李光宇", "2026/5/27 13:32", "甲", "", "乙", "", "6/1");
    });

    var rows = importer.parse(file("诊断时间.xlsx", workbook));
    assertEquals("沈阳沈大内窥镜有限公司", rows.get(0).name());
    assertEquals("大东区", rows.get(0).county());
    assertEquals(true, rows.get(0).visitsProvided());
    assertEquals("2026-05-13T13:56", rows.get(0).visit1At().toString());
    assertEquals("2026-05-22T13:32", rows.get(0).visit2At().toString());
    assertEquals("2026-06-12T09:00", rows.get(0).visit3At().toString());
    assertEquals("2026-05-27T13:32", rows.get(1).visit1At().toString());
    assertEquals(null, rows.get(1).visit2At());
    assertEquals(null, rows.get(1).visit3At());
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
    when(file.getBytes()).thenReturn(content);
    return file;
  }

  @FunctionalInterface
  private interface SheetWriter {
    void write(org.apache.poi.ss.usermodel.Sheet sheet);
  }
}
