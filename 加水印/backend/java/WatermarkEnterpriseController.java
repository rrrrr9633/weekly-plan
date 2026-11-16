package com.weeklyplan.watermark;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/watermark-enterprises")
public class WatermarkEnterpriseController {
  private final WatermarkEnterpriseService enterprises;

  public WatermarkEnterpriseController(WatermarkEnterpriseService enterprises) {
    this.enterprises = enterprises;
  }

  @GetMapping
  public List<WatermarkEnterpriseResponse> list() {
    return enterprises.list();
  }

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public WatermarkEnterpriseImportResult importWorkbook(@RequestPart("file") MultipartFile file) {
    return enterprises.importWorkbook(file);
  }
}
