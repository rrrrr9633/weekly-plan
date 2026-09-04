package com.weeklyplan.diagnosis;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DiagnosisWorkbookImportService {
  private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

  public List<ImportedDiagnosisRow> parse(MultipartFile file) {
    String filename = file == null ? "" : file.getOriginalFilename();
    if (file == null || file.isEmpty()) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "请选择xlsx文件");
    if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "仅支持xlsx文件");
    try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
      Sheet sheet = workbook.getSheetAt(0);
      Header header = locateHeader(sheet);
      List<ImportedDiagnosisRow> rows = new ArrayList<>();
      for (int rowIndex = header.rowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex); if (row == null) continue;
        String enterprise = text(row, header.enterprise); if (enterprise.isBlank()) continue;
        for (DateColumn dateColumn : header.dateColumns) {
          LocalDate date = date(row, dateColumn.column); if (date == null) continue;
          String diagnosisTime = header.diagnosisTime >= 0 ? text(row, header.diagnosisTime) : text(row, dateColumn.column);
          int diagnosisRound = dateColumn.round > 0 ? dateColumn.round : round(text(row, header.round));
          rows.add(new ImportedDiagnosisRow(date, enterprise, countyOnly(text(row, header.county)), diagnosisTime, diagnosisRound, text(row, header.contact), text(row, header.contactPhone), participants(row, header.participants)));
        }
      }
      return rows;
    } catch (IOException error) { throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "xlsx文件读取失败", error); }
  }

  private Header locateHeader(Sheet sheet) {
    for (Row row : sheet) {
      int date = -1, enterprise = -1, county = -1, diagnosisTime = -1, round = -1, contact = -1, contactPhone = -1, participants = -1;
      List<DateColumn> diagnosisDates = new ArrayList<>();
      for (Cell cell : row) { String value = formatter.formatCellValue(cell).trim(); if (value.equals("时间") || value.equals("日期") || value.equals("诊断日期")) date = cell.getColumnIndex(); if (value.contains("企业名称") || value.equals("企业")) enterprise = cell.getColumnIndex(); if (value.contains("县区") || value.contains("区（县）") || value.contains("区(县)") || value.equals("地址")) county = cell.getColumnIndex(); if (value.contains("诊断时间") && !value.matches("第[一二三四五六七八九十0-9]+次诊断时间")) diagnosisTime = cell.getColumnIndex(); if (value.contains("第几次") || value.contains("入企")) round = cell.getColumnIndex(); if (value.contains("企业联系人")) contact = cell.getColumnIndex(); if (value.contains("企业联系方式") || value.contains("企业联系电话")) contactPhone = cell.getColumnIndex(); if (value.contains("诊断人员1")) participants = cell.getColumnIndex(); int diagnosisRound = roundInHeader(value); if (diagnosisRound > 0 && value.contains("诊断时间")) diagnosisDates.add(new DateColumn(cell.getColumnIndex(), diagnosisRound)); }
      if (date >= 0) diagnosisDates.add(new DateColumn(date, -1));
      if (enterprise >= 0 && !diagnosisDates.isEmpty()) return new Header(row.getRowNum(), enterprise, county, diagnosisTime, round, contact, contactPhone, participants, diagnosisDates);
    }
    throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "未识别到企业名称和诊断日期列");
  }

  private String text(Row row, int column) { return column < 0 || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column)).trim(); }
  private List<String> participants(Row row, int column) { String value = text(row, column); return value.isBlank() ? List.of() : List.of(value.split("[,，、/\\s]+")); }
  private LocalDate date(Row row, int column) { if (column < 0 || row.getCell(column) == null) return null; Cell cell = row.getCell(column); if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); String value = text(row, column).replace('年', '-').replace('月', '-').replace("日", "").replace('/', '-'); try { return LocalDate.parse(value); } catch (Exception ignored) { return null; } }
  private int round(String value) { try { return Integer.parseInt(value.replaceAll("[^0-9]", "")); } catch (Exception ignored) { return 1; } }
  private String countyOnly(String value) { var matcher = java.util.regex.Pattern.compile("^(.{2,12}?(?:县|区|市辖区))").matcher(value); return matcher.find() ? matcher.group(1) : value; }
  private int roundInHeader(String value) { String digits = value.replaceAll("[^0-9]", ""); if (!digits.isBlank()) return Integer.parseInt(digits); String chinese = value.replaceAll("[^一二三四五六七八九十]", ""); return switch (chinese) { case "一" -> 1; case "二" -> 2; case "三" -> 3; case "四" -> 4; case "五" -> 5; default -> -1; }; }
  private record DateColumn(int column, int round) {}
  private record Header(int rowIndex, int enterprise, int county, int diagnosisTime, int round, int contact, int contactPhone, int participants, List<DateColumn> dateColumns) {}
}
