package com.weeklyplan.reportpdf;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/report-pdfs")
public class ReportPdfController {
  private final ReportPdfService reports;

  public ReportPdfController(ReportPdfService reports) {
    this.reports = reports;
  }

  @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> convert(
      @RequestPart("word") MultipartFile word,
      @RequestPart("cover") MultipartFile cover
  ) {
    byte[] pdf = reports.convert(word, cover);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(reports.outputName(word), StandardCharsets.UTF_8).build()
    );
    return ResponseEntity.ok().headers(headers).body(pdf);
  }
}
