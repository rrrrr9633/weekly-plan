package com.weeklyplan.watermark;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class WatermarkEnterpriseDeduper {
  record Plan(
      List<WatermarkEnterprise> keep,
      List<WatermarkEnterprise> drop,
      Map<WatermarkEnterprise, List<WatermarkEnterprise>> extras
  ) {}

  private WatermarkEnterpriseDeduper() {}

  static Plan plan(List<WatermarkEnterprise> rows) {
    List<WatermarkEnterprise> ordered = rows.stream()
        .sorted(Comparator.comparing(WatermarkEnterprise::getId, Comparator.nullsLast(Long::compareTo)))
        .toList();
    Map<String, WatermarkEnterprise> keepers = new LinkedHashMap<>();
    Map<WatermarkEnterprise, List<WatermarkEnterprise>> extras = new LinkedHashMap<>();
    for (WatermarkEnterprise row : ordered) {
      String key = WatermarkPlaceText.identity(row.getName(), row.getCity(), row.getCounty());
      WatermarkEnterprise keeper = keepers.get(key);
      if (keeper == null) {
        keepers.put(key, row);
        extras.put(row, new ArrayList<>());
        continue;
      }
      extras.get(keeper).add(row);
    }
    List<WatermarkEnterprise> drop = extras.values().stream().flatMap(List::stream).toList();
    return new Plan(List.copyOf(keepers.values()), List.copyOf(drop), Map.copyOf(extras));
  }

  static void mergeIntoKeepers(Plan plan) {
    plan.extras().forEach((keeper, duplicates) -> {
      for (WatermarkEnterprise duplicate : duplicates) keeper.absorb(duplicate);
    });
  }

  static WatermarkEnterprise match(
      List<WatermarkEnterprise> rows,
      String name,
      String city,
      String county
  ) {
    String key = WatermarkPlaceText.identity(name, city, county);
    return rows.stream()
        .filter(row -> Objects.equals(
            WatermarkPlaceText.identity(row.getName(), row.getCity(), row.getCounty()),
            key
        ))
        .findFirst()
        .orElse(null);
  }
}
