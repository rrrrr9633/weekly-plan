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
        LocalDate date = date(row, header.date); if (date == null) continue;
        rows.add(new ImportedDiagnosisRow(date, enterprise, countyOnly(text(row, header.county)), text(row, header.diagnosisTime), round(text(row, header.round)), text(row, header.contact), text(row, header.contactPhone), participants(row, header.participants)));
      }
      return rows;
    } catch (IOException error) { throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "xlsx文件读取失败", error); }
  }

  private Header locateHeader(Sheet sheet) {
    for (Row row : sheet) {
      int date = -1, enterprise = -1, county = -1, diagnosisTime = -1, round = -1, contact = -1, contactPhone = -1, participants = -1;
      for (Cell cell : row) { String value = formatter.formatCellValue(cell).trim(); if (value.equals("时间") || value.equals("日期") || value.equals("诊断日期")) date = cell.getColumnIndex(); if (value.contains("企业名称") || value.equals("企业")) enterprise = cell.getColumnIndex(); if (value.contains("县区") || value.contains("区（县）") || value.contains("区(县)") || value.equals("地址")) county = cell.getColumnIndex(); if (value.contains("诊断时间")) diagnosisTime = cell.getColumnIndex(); if (value.contains("第几次") || value.contains("入企")) round = cell.getColumnIndex(); if (value.contains("企业联系人")) contact = cell.getColumnIndex(); if (value.contains("企业联系方式") || value.contains("企业联系电话")) contactPhone = cell.getColumnIndex(); if (value.contains("诊断人员1")) participants = cell.getColumnIndex(); }
      if (date >= 0 && enterprise >= 0) return new Header(row.getRowNum(), date, enterprise, county, diagnosisTime, round, contact, contactPhone, participants);
    }
    throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "未识别到时间和企业名称列");
  }

  private String text(Row row, int column) { return column < 0 || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column)).trim(); }
  private List<String> participants(Row row, int column) { String value = text(row, column); return value.isBlank() ? List.of() : List.of(value.split("[,，、/\\s]+")); }
  private LocalDate date(Row row, int column) { if (column < 0 || row.getCell(column) == null) return null; Cell cell = row.getCell(column); if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); String value = text(row, column).replace('年', '-').replace('月', '-').replace("日", "").replace('/', '-'); try { return LocalDate.parse(value); } catch (Exception ignored) { return null; } }
  private int round(String value) { try { return Integer.parseInt(value.replaceAll("[^0-9]", "")); } catch (Exception ignored) { return 1; } }
  private String countyOnly(String value) { var matcher = java.util.regex.Pattern.compile("^(.{2,12}?(?:县|区|市辖区))").matcher(value); return matcher.find() ? matcher.group(1) : value; }
  private record Header(int rowIndex, int date, int enterprise, int county, int diagnosisTime, int round, int contact, int contactPhone, int participants) {}
}
