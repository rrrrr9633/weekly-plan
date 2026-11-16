package com.weeklyplan.watermark;

import java.time.LocalDateTime;

public record ImportedWatermarkEnterprise(
    String name,
    String city,
    String county,
    LocalDateTime visit1At,
    LocalDateTime visit2At,
    LocalDateTime visit3At,
    boolean visitsProvided
) {
  public ImportedWatermarkEnterprise(String name, String city, String county) {
    this(name, city, county, null, null, null, false);
  }
}
