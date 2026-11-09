package com.weeklyplan.diagnosis;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DiagnosisWorkbookImportService {
  private static final Pattern YEAR = Pattern.compile("20\\d{2}");
  private static final Pattern COUNTY = Pattern.compile("^(.{2,12}?(?:县|区|市辖区))");
  private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("uuuu-M-d");
  private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M-d");
  private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

  public List<ImportedDiagnosisRow> parse(MultipartFile file) {
    String filename = file == null || file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择xlsx文件");
    if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持xlsx文件");
    try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
      Sheet sheet = workbook.getSheetAt(0);
      int year = yearOf(filename, sheet.getSheetName());
      Header header = locateHeader(sheet);
      List<ImportedDiagnosisRow> rows = new ArrayList<>();
      for (int rowIndex = header.rowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;
        String enterprise = text(row, header.enterprise);
        if (enterprise.isBlank()) continue;
        String county = countyOnly(text(row, header.county));
        List<String> roster = names(texts(row, header.roster));
        for (DateColumn dateColumn : header.dateColumns) {
          LocalDate date = date(row, dateColumn.column, year);
          if (date == null) continue;
          String diagnosisTime = header.diagnosisTime >= 0 ? text(row, header.diagnosisTime) : text(row, dateColumn.column);
          if (diagnosisTime.isBlank()) diagnosisTime = date.toString();
          int diagnosisRound = dateColumn.round > 0 ? dateColumn.round : round(text(row, header.round));
          // 该次照片内人员优先；空着才退回这一行的诊断人员1、诊断人员2、专家
          List<String> participants = names(text(row, dateColumn.photoColumn));
          if (participants.isEmpty()) participants = roster;
          rows.add(new ImportedDiagnosisRow(date, enterprise, county, diagnosisTime, diagnosisRound, text(row, header.contact), text(row, header.contactPhone), participants));
        }
      }
      return rows;
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "xlsx文件读取失败", error);
    }
  }

  private Header locateHeader(Sheet sheet) {
    for (Row row : sheet) {
      int enterprise = -1, county = -1, diagnosisTime = -1, round = -1, contact = -1, contactPhone = -1;
      List<Integer> roster = new ArrayList<>();
      List<DateColumn> diagnosisDates = new ArrayList<>();
      for (Cell cell : row) {
        String value = headerText(cell);
        if (value.isBlank()) continue;
        int column = cell.getColumnIndex();
        if (value.equals("时间") || value.equals("日期") || value.equals("诊断日期")) diagnosisDates.add(new DateColumn(column, -1, -1));
        else if (isVisitDate(value)) diagnosisDates.add(new DateColumn(column, roundInHeader(value), -1));
        else if (value.contains("照片内人员") && !diagnosisDates.isEmpty()) {
          DateColumn last = diagnosisDates.get(diagnosisDates.size() - 1);
          if (last.photoColumn < 0) diagnosisDates.set(diagnosisDates.size() - 1, new DateColumn(last.column, last.round, column));
        } else if (value.contains("企业名称") || value.equals("企业")) enterprise = column;
        else if (value.contains("县区") || value.contains("区（县）") || value.contains("区(县)") || value.equals("地址")) county = column;
        else if (value.contains("诊断时间")) diagnosisTime = column;
        else if (value.contains("第几次") || value.contains("入企")) round = column;
        else if (value.contains("企业联系人")) contact = column;
        else if (value.contains("企业联系方式") || value.contains("企业联系电话")) contactPhone = column;
        else if (value.contains("诊断人员") || value.equals("专家")) roster.add(column);
      }
      if (enterprise >= 0 && !diagnosisDates.isEmpty()) return new Header(row.getRowNum(), enterprise, county, diagnosisTime, round, contact, contactPhone, roster, List.copyOf(diagnosisDates));
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到企业名称和诊断日期列");
  }

  private boolean isVisitDate(String value) {
    return value.contains("诊断时间") && roundInHeader(value) > 0;
  }

  private String headerText(Cell cell) {
    return formatter.formatCellValue(cell).replaceAll("\\s+", "");
  }

  private String text(Row row, int column) {
    return column < 0 || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column)).trim();
  }

  private List<String> texts(Row row, List<Integer> columns) {
    List<String> values = new ArrayList<>();
    for (int column : columns) values.add(text(row, column));
    return values;
  }

  private List<String> names(String raw) {
    return names(List.of(raw));
  }

  private List<String> names(List<String> rawValues) {
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (String raw : rawValues) {
      if (raw == null || raw.isBlank() || isBlankMarker(raw)) continue;
      for (String part : raw.split("[,，、/|;；\\s]+")) {
        String name = part.trim();
        if (!name.isBlank() && !isBlankMarker(name)) names.add(name);
      }
    }
    return List.copyOf(names);
  }

  private boolean isBlankMarker(String value) {
    return switch (value.trim()) {
      case "缺失", "无", "暂无", "空", "-", "—", "——", "/", "／" -> true;
      default -> false;
    };
  }

  private LocalDate date(Row row, int column, int year) {
    if (column < 0 || row.getCell(column) == null) return null;
    Cell cell = row.getCell(column);
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    String value = text(row, column);
    if (value.isBlank() || isBlankMarker(value)) return null;
    String normalized = value.replace('年', '-').replace('月', '-').replace("日", "").replace('/', '-').replace('／', '-').replace('.', '-').replaceAll("\\s+", "");
    try {
      if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) return LocalDate.parse(normalized, FULL_DATE);
      if (normalized.matches("\\d{1,2}-\\d{1,2}")) return MonthDay.parse(normalized, MONTH_DAY).atYear(year);
    } catch (DateTimeParseException ignored) {
      return null;
    }
    return null;
  }

  private int yearOf(String filename, String sheetName) {
    Matcher inFile = YEAR.matcher(filename == null ? "" : filename);
    if (inFile.find()) return Integer.parseInt(inFile.group());
    Matcher inSheet = YEAR.matcher(sheetName == null ? "" : sheetName);
    if (inSheet.find()) return Integer.parseInt(inSheet.group());
    return LocalDate.now(ZoneId.systemDefault()).getYear();
  }

  private int round(String value) {
    try { return Integer.parseInt(value.replaceAll("[^0-9]", "")); }
    catch (Exception ignored) { return 1; }
  }

  private String countyOnly(String value) {
    Matcher matcher = COUNTY.matcher(value);
    return matcher.find() ? matcher.group(1) : value;
  }

  private int roundInHeader(String value) {
    String digits = value.replaceAll("[^0-9]", "");
    if (!digits.isBlank()) return Integer.parseInt(digits);
    String chinese = value.replaceAll("[^一二三四五六七八九十]", "");
    return switch (chinese) {
      case "一" -> 1;
      case "二" -> 2;
      case "三" -> 3;
      case "四" -> 4;
      case "五" -> 5;
      default -> -1;
    };
  }

  private record DateColumn(int column, int round, int photoColumn) {}

  private record Header(int rowIndex, int enterprise, int county, int diagnosisTime, int round, int contact, int contactPhone, List<Integer> roster, List<DateColumn> dateColumns) {}
}
