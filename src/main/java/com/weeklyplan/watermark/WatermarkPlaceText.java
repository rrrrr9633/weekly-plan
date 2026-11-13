package com.weeklyplan.watermark;

final class WatermarkPlaceText {
  private WatermarkPlaceText() {}

  static String clean(String raw) {
    if (raw == null) return "";
    return raw.replace('\u00a0', ' ')
        .replace('\u3000', ' ')
        .replaceAll("\\s+", " ")
        .trim();
  }

  static String identity(String name, String city, String county) {
    return clean(name) + '\u0000' + cityKey(city) + '\u0000' + countyKey(county);
  }

  static boolean samePlace(String city1, String county1, String city2, String county2) {
    return cityKey(city1).equals(cityKey(city2)) && countyKey(county1).equals(countyKey(county2));
  }

  static String cityKey(String city) {
    return stripSuffix(clean(city), "市辖区", "地区", "自治州", "盟", "市");
  }

  static String countyKey(String county) {
    return stripSuffix(clean(county), "市辖区", "自治县", "自治旗", "新区", "区", "县", "旗", "市");
  }

  private static String stripSuffix(String value, String... suffixes) {
    for (String suffix : suffixes) {
      if (value.length() > suffix.length() && value.endsWith(suffix)) {
        return value.substring(0, value.length() - suffix.length());
      }
    }
    return value;
  }
}
