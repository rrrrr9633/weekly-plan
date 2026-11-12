package com.weeklyplan.watermark;

public record WatermarkEnterpriseResponse(String id, String name, String city, String county) {
  static WatermarkEnterpriseResponse of(WatermarkEnterprise row) {
    return new WatermarkEnterpriseResponse(String.valueOf(row.getId()), row.getName(), row.getCity(), row.getCounty());
  }
}
