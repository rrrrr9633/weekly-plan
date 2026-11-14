package com.weeklyplan.reportpdf;

import com.weeklyplan.module.FeatureModuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class ReportPdfService {
  static final MediaType DOCX = MediaType.parseMediaType(
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  );
  private static final String MODULE = "REPORT_COVER_PDF";
  private final FeatureModuleService modules;
  private final LibreOfficePdfConverter converter;
  private final CoverPdfComposer pdfComposer;
  private final CoverWordComposer wordComposer;

  public ReportPdfService(
      FeatureModuleService modules,
      LibreOfficePdfConverter converter,
      CoverPdfComposer pdfComposer,
      CoverWordComposer wordComposer
  ) {
    this.modules = modules;
    this.converter = converter;
    this.pdfComposer = pdfComposer;
    this.wordComposer = wordComposer;
  }

  public record Export(byte[] bytes, String filename, MediaType mediaType) {}

  public Export convert(MultipartFile report, MultipartFile cover, String format) {
    return convert(report, cover, format, null);
  }

  public Export convert(MultipartFile report, MultipartFile cover, String format, ReportPdfProgress progress) {
    modules.requireEnabled(MODULE);
    if (report == null || report.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传 Word 或 PDF 报告");
    if (cover == null || cover.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传封面图片");
    String reportName = filename(report);
    String coverName = filename(cover);
    if (!isImage(coverName)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面请上传 jpg 或 png");
    boolean asWord = isWordFormat(format);
    tick(progress, 18, "saving", "正在接收文件");
    try {
      byte[] reportBytes = report.getBytes();
      byte[] coverBytes = cover.getBytes();
      if (isPdf(reportName, reportBytes)) {
        if (asWord) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF 报告只能导出 PDF，请改选 PDF");
        tick(progress, 40, "composing", "PDF 不用转 Word，正在替换第 1 页");
        return new Export(
            pdfComposer.replaceFirstPage(reportBytes, coverBytes, progress),
            outputName(reportName, false),
            MediaType.APPLICATION_PDF
        );
      }
      if (!isWord(reportName)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报告请上传 .doc、.docx 或 .pdf");
      Path work = Files.createTempDirectory("report-pdf-");
      try {
        Path input = work.resolve(safeName(reportName, ".docx"));
        Files.write(input, reportBytes);
        reportBytes = null;
        if (asWord) {
          tick(progress, 30, "converting", "正在准备 Word");
          byte[] docx = converter.convertToDocx(input, work, progress);
          tick(progress, 82, "composing", "正在把封面插入 Word 第 1 页");
          return new Export(wordComposer.replaceFirstPage(docx, coverBytes), outputName(reportName, true), DOCX);
        }
        tick(progress, 26, "converting", "正在把 Word 转成 PDF，这一步最慢");
        Path pdf = converter.convertWordToFile(input, work, progress);
        return new Export(
            pdfComposer.replaceFirstPage(pdf, coverBytes, progress),
            outputName(reportName, false),
            MediaType.APPLICATION_PDF
        );
      } finally {
        deleteQuietly(work);
      }
    } catch (ResponseStatusException error) {
      throw error;
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法处理上传的报告或封面图");
    }
  }

  public String outputName(String originalName, boolean asWord) {
    String name = originalName == null ? "" : originalName.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    int dot = name.lastIndexOf('.');
    String stem = dot > 0 ? name.substring(0, dot) : (name.isBlank() ? "报告" : name);
    return stem + (asWord ? "-盖章封面.docx" : "-盖章封面.pdf");
  }

  public String outputName(MultipartFile word, boolean asWord) {
    return outputName(filename(word), asWord);
  }

  private void tick(ReportPdfProgress progress, int percent, String stage, String message) {
    if (progress != null) progress.update(percent, stage, message);
  }

  private boolean isWordFormat(String format) {
    String value = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
    return value.equals("word") || value.equals("docx");
  }

  private boolean isWord(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".doc") || lower.endsWith(".docx");
  }

  private boolean isPdf(String name, byte[] bytes) {
    if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) return true;
    return bytes != null && bytes.length >= 5
        && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-';
  }

  private boolean isImage(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
  }

  private String filename(MultipartFile file) {
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    return name.replace('\\', '/');
  }

  private String safeName(String original, String fallbackExt) {
    String base = original.substring(original.lastIndexOf('/') + 1).replaceAll("[^\\w.\\u4e00-\\u9fff-]+", "_");
    if (base.isBlank()) base = "report" + fallbackExt;
    return base;
  }

  private void deleteQuietly(Path work) {
    if (work == null || !Files.exists(work)) return;
    try (Stream<Path> paths = Files.walk(work)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
      });
    } catch (IOException ignored) {
    }
  }
}
