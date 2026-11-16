package com.weeklyplan.reportpdf;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/report-pdfs")
public class ReportPdfController {
  private final ReportPdfService reports;
  private final ReportPdfJobService jobs;

  public ReportPdfController(ReportPdfService reports, ReportPdfJobService jobs) {
    this.reports = reports;
    this.jobs = jobs;
  }

  @PostMapping(value = "/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ReportPdfJobService.Status start(
      @RequestPart("word") MultipartFile word,
      @RequestPart("cover") MultipartFile cover,
      @RequestParam(value = "format", defaultValue = "pdf") String format
  ) {
    return jobs.start(word, cover, format);
  }

  @GetMapping("/jobs/{id}")
  public ReportPdfJobService.Status status(@PathVariable String id) {
    return jobs.status(id);
  }

  @GetMapping("/jobs/{id}/file")
  public ResponseEntity<org.springframework.core.io.Resource> file(@PathVariable String id) {
    ReportPdfJobService.JobFile export = jobs.file(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(export.mediaType());
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(export.filename(), StandardCharsets.UTF_8).build()
    );
    return ResponseEntity.ok().headers(headers).body(export.resource());
  }

  @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> convert(
      @RequestPart("word") MultipartFile word,
      @RequestPart("cover") MultipartFile cover,
      @RequestParam(value = "format", defaultValue = "pdf") String format
  ) {
    ReportPdfService.Export export = reports.convert(word, cover, format);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(export.mediaType());
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(export.filename(), StandardCharsets.UTF_8).build()
    );
    return ResponseEntity.ok().headers(headers).body(export.bytes());
  }
}
