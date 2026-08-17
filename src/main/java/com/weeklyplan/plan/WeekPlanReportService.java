package com.weeklyplan.plan;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;

@Service
public class WeekPlanReportService {
  private static final int SUMMARY_START_ROW = 4;
  private static final int SUMMARY_TEMPLATE_ROWS = 3;
  private static final int NEXT_WEEK_START_ROW = 7;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private final WeekPlanRepository plans;

  public WeekPlanReportService(WeekPlanRepository plans) {
    this.plans = plans;
  }

  @Transactional(readOnly = true)
  public byte[] exportMine(Long userId, int year, int weekNumber) {
    List<WeekPlan> weekPlans = plans.findParticipatingByUserAndWeek(userId, year, weekNumber).stream()
        .sorted((left, right) -> Integer.compare(weekdayRank(left.getWeekday()), weekdayRank(right.getWeekday())))
        .toList();

    try (InputStream template = new ClassPathResource("templates/周报模版.xlsx").getInputStream();
         Workbook workbook = new XSSFWorkbook(template);
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.getSheetAt(0);
      expandSummaryRows(sheet, weekPlans.size());
      fillSummary(sheet, weekPlans, year, weekNumber);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException error) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "生成周报失败", error);
    }
  }

  private void expandSummaryRows(Sheet sheet, int planCount) {
    int requiredRows = Math.max(SUMMARY_TEMPLATE_ROWS, planCount);
    int rowsToInsert = requiredRows - SUMMARY_TEMPLATE_ROWS;
    if (rowsToInsert <= 0) return;

    sheet.shiftRows(NEXT_WEEK_START_ROW, sheet.getLastRowNum(), rowsToInsert, true, false);
    Row templateRow = sheet.getRow(SUMMARY_START_ROW + SUMMARY_TEMPLATE_ROWS - 1);
    for (int offset = 1; offset <= rowsToInsert; offset++) {
      copyRow(sheet, templateRow, SUMMARY_START_ROW + SUMMARY_TEMPLATE_ROWS - 1 + offset);
    }
  }

  private void fillSummary(Sheet sheet, List<WeekPlan> plans, int year, int weekNumber) {
    LocalDate weekStart = LocalDate.of(year, 1, 4)
        .with(WeekFields.ISO.weekOfWeekBasedYear(), weekNumber)
        .with(WeekFields.ISO.dayOfWeek(), 1);
    sheet.getRow(2).getCell(1).setCellValue("本周工作总结（" + DATE_FORMAT.format(weekStart) + " 至 " + DATE_FORMAT.format(weekStart.plusDays(6)) + "）");

    for (int index = 0; index < plans.size(); index++) {
      WeekPlan plan = plans.get(index);
      Row row = sheet.getRow(SUMMARY_START_ROW + index);
      cell(row, 1).setCellValue(index + 1);
      cell(row, 2).setCellValue(formatContent(plan, weekStart));
      cell(row, 3).setCellValue(plan.getStatus() == PlanStatus.ARCHIVED ? "已完成" : "");
      cell(row, 4).setCellValue("");
    }
  }

  private String formatContent(WeekPlan plan, LocalDate weekStart) {
    String date = plan.getWeekday() == PlanWeekday.PENDING
        ? "待排期"
        : DATE_FORMAT.format(weekStart.plusDays(plan.getWeekday().ordinal()));
    return plan.getProject().getName() + " / " + plan.getContent() + " / " + date;
  }

  private int weekdayRank(PlanWeekday weekday) {
    return weekday == PlanWeekday.PENDING ? 0 : weekday.ordinal() + 1;
  }

  private Cell cell(Row row, int columnIndex) {
    Cell cell = row.getCell(columnIndex);
    return cell == null ? row.createCell(columnIndex) : cell;
  }

  private void copyRow(Sheet sheet, Row source, int targetIndex) {
    Row target = sheet.createRow(targetIndex);
    target.setHeight(source.getHeight());
    for (int columnIndex = source.getFirstCellNum(); columnIndex < source.getLastCellNum(); columnIndex++) {
      Cell sourceCell = source.getCell(columnIndex);
      if (sourceCell == null) continue;
      Cell targetCell = target.createCell(columnIndex);
      CellStyle style = sourceCell.getCellStyle();
      targetCell.setCellStyle(style);
    }
  }
}
