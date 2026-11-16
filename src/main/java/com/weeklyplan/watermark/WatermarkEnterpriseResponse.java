package com.weeklyplan.watermark;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record WatermarkEnterpriseResponse(
    String id,
    String name,
    String city,
    String county,
    String visit1At,
    String visit2At,
    String visit3At
) {
  static WatermarkEnterpriseResponse of(WatermarkEnterprise row) {
    return new WatermarkEnterpriseResponse(
        String.valueOf(row.getId()),
        row.getName(),
        row.getCity(),
        row.getCounty(),
        iso(row.getVisit1At()),
        iso(row.getVisit2At()),
        iso(row.getVisit3At())
    );
  }

  private static String iso(LocalDateTime time) {
    return time == null ? null : time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}
