package com.weeklyplan.watermark;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class WatermarkEnterpriseWorkbookService {
  private static final Pattern CITY = Pattern.compile("^.{1,12}市$");
  private static final Pattern COUNTY = Pattern.compile("^.{1,20}(?:市辖区|新区|区|县|旗)$");
  private static final Pattern COMPANY = Pattern.compile("公司|集团|厂|药业|企业");
  private static final DateTimeFormatter[] DATE_TIMES = {
      DateTimeFormatter.ISO_LOCAL_DATE_TIME,
      formatter("yyyy-M-d'T'H:mm[:ss]"),
      formatter("yyyy-M-d H:mm[:ss]"),
      formatter("yyyy/M/d H:mm[:ss]"),
      formatter("yyyy.M.d H:mm[:ss]"),
  };
  private static final DateTimeFormatter[] DATES = {
      DateTimeFormatter.ISO_LOCAL_DATE,
      formatter("yyyy-M-d"),
      formatter("yyyy/M/d"),
      formatter("yyyy.M.d"),
  };
  private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

  public List<ImportedWatermarkEnterprise> parse(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择表格文件");
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    try {
      byte[] bytes = file.getBytes();
      if (filename.endsWith(".csv") || filename.endsWith(".txt") || looksLikeText(bytes)) return parseGrid(readCsv(bytes));
      try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
        return parseSheet(workbook.getSheetAt(0));
      }
    } catch (ResponseStatusException error) {
      throw error;
    } catch (Exception error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取表格，请导出 xlsx / xls / csv。当前文件不是可识别的表格。");
    }
  }

  private List<ImportedWatermarkEnterprise> parseSheet(Sheet sheet) {
    if (sheet == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "表格是空的");
    List<List<String>> grid = new ArrayList<>();
    for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) continue;
      List<String> values = new ArrayList<>();
      int last = row.getLastCellNum();
      for (int column = 0; column < last; column++) values.add(text(row, column));
      if (values.stream().anyMatch(value -> !value.isBlank())) grid.add(values);
    }
    return parseGrid(grid);
  }

  List<ImportedWatermarkEnterprise> parseGrid(List<List<String>> grid) {
    Header header = locateHeader(grid);
    Map<String, ImportedWatermarkEnterprise> unique = new LinkedHashMap<>();
    boolean visitsProvided = header.visit1 >= 0 || header.visit2 >= 0 || header.visit3 >= 0;
    for (int rowIndex = header.rowIndex + 1; rowIndex < grid.size(); rowIndex++) {
      List<String> row = grid.get(rowIndex);
      String name = cell(row, header.name);
      if (name.isBlank()) continue;
      String city = cell(row, header.city);
      String county = cell(row, header.county);
      LocalDateTime visit1 = parseVisitTime(cell(row, header.visit1));
      LocalDateTime visit2 = parseVisitTime(cell(row, header.visit2));
      LocalDateTime visit3 = parseVisitTime(cell(row, header.visit3));
      unique.put(
          name + '\u0000' + city + '\u0000' + county,
          new ImportedWatermarkEnterprise(name, city, county, visit1, visit2, visit3, visitsProvided)
      );
    }
    if (unique.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有解析到企业名称和区县，请确认表格含企业名称、市、区县");
    return List.copyOf(unique.values());
  }

  static LocalDateTime parseVisitTime(String raw) {
    if (raw == null) return null;
    String value = raw.trim().replace('\u00a0', ' ');
    if (value.isBlank() || value.equals("-") || value.equals("/") || value.equals("无")) return null;
    value = value.replace('年', '-').replace('月', '-').replace("日", " ").replace('.', '/').replaceAll("\\s+", " ").trim();
    for (DateTimeFormatter format : DATE_TIMES) {
      try {
        return LocalDateTime.parse(value, format);
      } catch (Exception ignored) {
      }
    }
    String datePart = value.split(" ")[0];
    for (DateTimeFormatter format : DATES) {
      try {
        return LocalDate.parse(datePart, format).atStartOfDay();
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  private Header locateHeader(List<List<String>> grid) {
    for (int rowIndex = 0; rowIndex < Math.min(grid.size(), 8); rowIndex++) {
      List<String> row = grid.get(rowIndex);
      int name = -1, city = -1, county = -1, visit1 = -1, visit2 = -1, visit3 = -1;
      for (int column = 0; column < row.size(); column++) {
        String value = compact(row.get(column));
        if (value.isBlank()) continue;
        if (value.contains("企业名称") || value.equals("企业")) name = column;
        else if (value.equals("市") || value.equals("城市") || value.contains("地市")) city = column;
        else if (value.contains("县区") || value.contains("区县") || value.contains("区（县）") || value.contains("区(县)")) county = column;
        else if (isVisitHeader(value, "一") || isVisitHeader(value, "1")) visit1 = column;
        else if (isVisitHeader(value, "二") || isVisitHeader(value, "2")) visit2 = column;
        else if (isVisitHeader(value, "三") || isVisitHeader(value, "3")) visit3 = column;
      }
      if (name >= 0 && county >= 0) return new Header(rowIndex, name, city, county, visit1, visit2, visit3);
    }
    Header inferred = inferHeader(grid);
    if (inferred != null) return inferred;
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到企业名称和区县列。请用带表头的企业清单：企业名称、市、区(县)、三次诊断时间。");
  }

  private boolean isVisitHeader(String value, String ordinal) {
    if (value.contains("报告") || value.contains("人员") || value.contains("诊断人")) return false;
    return value.contains("第" + ordinal) && (value.contains("时间") || value.contains("诊断"));
  }

  private Header inferHeader(List<List<String>> grid) {
    int width = grid.stream().mapToInt(List::size).max().orElse(0);
    if (width < 2) return null;
    int[] names = new int[width], cities = new int[width], counties = new int[width];
    int sample = Math.min(grid.size(), 40);
    for (int rowIndex = 0; rowIndex < sample; rowIndex++) {
      List<String> row = grid.get(rowIndex);
      for (int column = 0; column < row.size(); column++) {
        String value = compact(row.get(column));
        if (COMPANY.matcher(value).find() && !value.contains("制造")) names[column]++;
        if (CITY.matcher(value).matches() && !COMPANY.matcher(value).find()) cities[column]++;
        if (COUNTY.matcher(value).matches() && !CITY.matcher(value).matches()) counties[column]++;
      }
    }
    int name = best(names, -1, -1);
    int city = best(cities, name, -1);
    int county = best(counties, name, city);
    if (name < 0 || county < 0) return null;
    return new Header(-1, name, city, county, -1, -1, -1);
  }

  private int best(int[] scores, int skipA, int skipB) {
    int best = -1;
    for (int i = 0; i < scores.length; i++) {
      if (i == skipA || i == skipB) continue;
      if (scores[i] > 0 && (best < 0 || scores[i] > scores[best])) best = i;
    }
    return best;
  }

  private String text(Row row, int column) {
    Cell cell = row.getCell(column);
    if (cell == null) return "";
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      LocalDateTime time = cell.getLocalDateTimeCellValue();
      return time == null ? "" : time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    return formatter.formatCellValue(cell).trim().replace('\n', ' ');
  }

  private String cell(List<String> row, int column) {
    if (column < 0 || column >= row.size()) return "";
    return row.get(column).trim().replace('\n', ' ');
  }

  private String compact(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "");
  }

  private boolean looksLikeText(byte[] bytes) {
    if (bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K') return false;
    if (bytes.length >= 8 && bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF) return false;
    String head = new String(bytes, 0, Math.min(bytes.length, 200), StandardCharsets.UTF_8);
    return head.contains("公司") || head.contains(",") || head.contains("\t");
  }

  private List<List<String>> readCsv(byte[] bytes) {
    String text = decode(bytes).replace("\r\n", "\n").replace('\r', '\n');
    String[] lines = text.split("\n");
    List<List<String>> grid = new ArrayList<>();
    for (String line : lines) {
      if (line.isBlank()) continue;
      String delimiter = line.contains("\t") ? "\t" : ",";
      List<String> row = new ArrayList<>();
      for (String item : line.split(delimiter, -1)) row.add(item.trim().replaceAll("^\"|\"$", ""));
      grid.add(row);
    }
    return grid;
  }

  private String decode(byte[] bytes) {
    String utf8 = new String(bytes, StandardCharsets.UTF_8);
    if (!utf8.contains("\uFFFD")) return utf8;
    return new String(bytes, Charset.forName("GBK"));
  }

  private static DateTimeFormatter formatter(String pattern) {
    return new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern(pattern)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .toFormatter(Locale.CHINA);
  }

  private record Header(int rowIndex, int name, int city, int county, int visit1, int visit2, int visit3) {}
}
