package com.weeklyplan.diagnosis;

final class ImportConflicts {
  enum Action { INSERT, REPLACE, KEEP_OLD, NEED_CHOICE, INVALID }

  record Decision(Action action, String reason) {}

  private ImportConflicts() {}

  static Decision decide(boolean sameRound, String keep) {
    if (!sameRound) return new Decision(Action.INSERT, "");
    String choice = canonical(keep);
    if (choice.isEmpty()) return new Decision(Action.NEED_CHOICE, "请选择保留新的或旧的");
    if ("新".equals(choice)) return new Decision(Action.REPLACE, "");
    if ("旧".equals(choice)) return new Decision(Action.KEEP_OLD, "");
    return new Decision(Action.INVALID, "保留选择只能是新的或旧的");
  }

  private static String canonical(String keep) {
    if (keep == null) return "";
    return switch (keep.trim()) {
      case "新", "新的", "保留新的" -> "新";
      case "旧", "旧的", "保留旧的" -> "旧";
      default -> keep.trim();
    };
  }
}
