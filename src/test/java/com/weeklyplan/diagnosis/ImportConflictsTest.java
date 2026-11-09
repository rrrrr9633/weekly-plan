package com.weeklyplan.diagnosis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportConflictsTest {
  @Test
  void insertsWhenTheRoundIsNotAlreadyThere() {
    assertEquals(ImportConflicts.Action.INSERT, ImportConflicts.decide(false, null).action());
    assertEquals(ImportConflicts.Action.INSERT, ImportConflicts.decide(false, "新").action());
  }

  @Test
  void requiresAnExplicitChoiceForTheSameRound() {
    ImportConflicts.Decision missing = ImportConflicts.decide(true, "  ");
    assertEquals(ImportConflicts.Action.NEED_CHOICE, missing.action());
    assertEquals("请选择保留新的或旧的", missing.reason());
    assertEquals(ImportConflicts.Action.INVALID, ImportConflicts.decide(true, "合并").action());
  }

  @Test
  void acceptsEitherSideOfTheSameRound() {
    assertEquals(ImportConflicts.Action.REPLACE, ImportConflicts.decide(true, "保留新的").action());
    assertEquals(ImportConflicts.Action.KEEP_OLD, ImportConflicts.decide(true, "旧").action());
  }
}
