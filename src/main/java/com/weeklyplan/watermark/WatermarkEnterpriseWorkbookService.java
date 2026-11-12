package com.weeklyplan.watermark;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WatermarkEnterpriseWorkbookService {
  private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

  public List<ImportedWatermarkEnterprise> parse(MultipartFile file) {
    String filename = file == null || file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择xlsx文件");
    if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持xlsx文件");
    try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
      Sheet sheet = workbook.getSheetAt(0);
      Header header = locateHeader(sheet);
      Map<String, ImportedWatermarkEnterprise> unique = new LinkedHashMap<>();
      for (int rowIndex = header.rowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;
        String name = text(row, header.name);
        if (name.isBlank()) continue;
        String city = text(row, header.city);
        String county = text(row, header.county);
        unique.putIfAbsent(key(name, city, county), new ImportedWatermarkEnterprise(name, city, county));
      }
      return List.copyOf(unique.values());
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "xlsx文件读取失败", error);
    }
  }

  private Header locateHeader(Sheet sheet) {
    for (Row row : sheet) {
      int name = -1, city = -1, county = -1;
      for (Cell cell : row) {
        String value = formatter.formatCellValue(cell).replaceAll("\\s+", "");
        if (value.isBlank()) continue;
        int column = cell.getColumnIndex();
        if (value.contains("企业名称") || value.equals("企业")) name = column;
        else if (value.equals("市") || value.equals("城市") || value.contains("地市")) city = column;
        else if (value.contains("县区") || value.contains("区县") || value.contains("区（县）") || value.contains("区(县)") || value.equals("区县")) county = column;
      }
      if (name >= 0 && county >= 0) return new Header(row.getRowNum(), name, city, county);
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到企业名称和区县列");
  }

  private String text(Row row, int column) {
    if (column < 0 || row.getCell(column) == null) return "";
    return formatter.formatCellValue(row.getCell(column)).trim();
  }

  private String key(String name, String city, String county) {
    return name + '\u0000' + city + '\u0000' + county;
  }

  private record Header(int rowIndex, int name, int city, int county) {}
}
