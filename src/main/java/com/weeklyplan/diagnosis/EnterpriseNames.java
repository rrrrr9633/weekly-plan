package com.weeklyplan.diagnosis;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EnterpriseNames {
  static final int SIMILAR_PERCENT = 80;
  private static final String[] SUFFIXES = {"股份有限公司", "有限责任公司", "有限公司", "集团公司", "集团", "公司"};

  private EnterpriseNames() {}

  static boolean same(String left, String right) {
    String a = normalize(left);
    String b = normalize(right);
    return !a.isEmpty() && a.equals(b);
  }

  static Match best(String incoming, List<String> existing) {
    String matched = "";
    int best = 0;
    for (String name : existing) {
      int score = similarity(incoming, name);
      if (score > best) {
        best = score;
        matched = name;
      }
    }
    if (best == 100) return new Match("相同", 100, matched);
    if (best >= SIMILAR_PERCENT) return new Match("相似", best, matched);
    return new Match("新", 0, "");
  }

  static int similarity(String left, String right) {
    String a = normalize(left);
    String b = normalize(right);
    if (a.isEmpty() || b.isEmpty()) return 0;
    if (a.equals(b)) return 100;
    return Math.min(99, Math.max(dice(a, b), levenshteinPercent(a, b)));
  }

  static String normalize(String raw) {
    if (raw == null) return "";
    String value = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    value = value.replaceAll("[()（）\\[\\]【】《》\"'“”‘’·.。,，、/\\\\|_\\-—–]+", "");
    String stripped = stripSuffix(value);
    return stripped.isBlank() ? value : stripped;
  }

  private static String stripSuffix(String value) {
    boolean changed = true;
    while (changed) {
      changed = false;
      for (String suffix : SUFFIXES) {
        if (value.length() > suffix.length() && value.endsWith(suffix)) {
          value = value.substring(0, value.length() - suffix.length());
          changed = true;
          break;
        }
      }
    }
    return value;
  }

  private static int dice(String a, String b) {
    if (a.length() < 2 || b.length() < 2) return 0;
    Map<String, Integer> left = bigrams(a);
    int overlap = 0;
    for (var entry : bigrams(b).entrySet()) overlap += Math.min(entry.getValue(), left.getOrDefault(entry.getKey(), 0));
    return (int) Math.round(200.0 * overlap / (a.length() - 1 + b.length() - 1));
  }

  private static Map<String, Integer> bigrams(String value) {
    Map<String, Integer> counts = new HashMap<>();
    for (int i = 0; i < value.length() - 1; i++) counts.merge(value.substring(i, i + 2), 1, Integer::sum);
    return counts;
  }

  private static int levenshteinPercent(String a, String b) {
    int max = Math.max(a.length(), b.length());
    return (int) Math.round(100.0 * (max - distance(a, b)) / max);
  }

  private static int distance(String a, String b) {
    int[] previous = new int[b.length() + 1];
    int[] current = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) previous[j] = j;
    for (int i = 1; i <= a.length(); i++) {
      current[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[b.length()];
  }

  record Match(String nameMatch, int similarity, String matchedEnterpriseName) {}
}
