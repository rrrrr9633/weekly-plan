package com.weeklyplan.diagnosis;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiagnosisWorkbookImportServiceTest {
  private final DiagnosisWorkbookImportService importer = new DiagnosisWorkbookImportService();

  @Test
  void expandsEachVisitAndPrefersPhotoNames() throws Exception {
    byte[] workbook = workbook(sheet -> {
      String[] header = {
          "序号", "企业名称", "第几批", "市", "区（县）", "诊断人员1", "诊断人员2", "专家",
          "第一次诊断时间", "照片内人员", "第二次诊断时间", "照片内人员", "第三次诊断时间", "照片内人员",
          "报告编制日期", "诊断记录表是否签章（我方）", "诊断报告是否签章（企业）", "诊断报告是否盖章（我方）", "诊断报告是否盖章（企业）"
      };
      writeRow(sheet, 0, header);
      writeRow(sheet, 1, "1", "沈阳海龟医疗科技有限公司", "—", "沈阳", "沈北新区", "佟健翔 李光宇 徐婷", "徐婷", "徐婷", "5/2", "佟健翔、李光宇、徐婷", "6/16", "李光宇、黄欢", "6/25", "黄欢、董老师、胡淑丽", "7/1", "是", "是", "是", "是");
      writeRow(sheet, 2, "2", "只有花名册的企业", "—", "沈阳", "新民市", "张三 李四", "王五", "赵六", "5/5", "", "缺失", "", "", "", "", "否", "否", "否", "否");
      writeRow(sheet, 3, "3", "三次都缺失的企业", "—", "沈阳", "康平县", "张三", "", "", "缺失", "", "缺失", "", "缺失", "", "", "否", "否", "否", "否");
    });

    List<ImportedDiagnosisRow> rows = importer.parse(file("诊断台账2026.xlsx", workbook));

    assertEquals(4, rows.size());
    assertEquals(new ImportedDiagnosisRow(LocalDate.of(2026, 5, 2), "沈阳海龟医疗科技有限公司", "沈北新区", "5/2", 1, "", "", List.of("佟健翔", "李光宇", "徐婷")), rows.get(0));
    assertEquals(new ImportedDiagnosisRow(LocalDate.of(2026, 6, 16), "沈阳海龟医疗科技有限公司", "沈北新区", "6/16", 2, "", "", List.of("李光宇", "黄欢")), rows.get(1));
    assertEquals(new ImportedDiagnosisRow(LocalDate.of(2026, 6, 25), "沈阳海龟医疗科技有限公司", "沈北新区", "6/25", 3, "", "", List.of("黄欢", "董老师", "胡淑丽")), rows.get(2));
    assertEquals(new ImportedDiagnosisRow(LocalDate.of(2026, 5, 5), "只有花名册的企业", "新民市", "5/5", 1, "", "", List.of("张三", "李四", "王五", "赵六")), rows.get(3));
  }

  @Test
  void stillReadsTheOldSingleDateTemplate() throws Exception {
    byte[] workbook = workbook(sheet -> {
      writeRow(sheet, 0, "时间", "企业名称", "区（县）", "诊断时间", "第几次入企", "企业联系人", "企业联系方式", "诊断人员1");
      writeRow(sheet, 1, "2026-05-02", "旧企业", "和平区", "上午", "2", "联系人", "13800000000", "张三、李四");
    });

    List<ImportedDiagnosisRow> rows = importer.parse(file("旧表.xlsx", workbook));

    assertEquals(List.of(new ImportedDiagnosisRow(LocalDate.of(2026, 5, 2), "旧企业", "和平区", "上午", 2, "联系人", "13800000000", List.of("张三", "李四"))), rows);
  }

  private byte[] workbook(SheetWriter writer) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      writer.write(workbook.createSheet("诊断"));
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
