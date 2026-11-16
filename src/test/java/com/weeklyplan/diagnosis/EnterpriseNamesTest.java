package com.weeklyplan.diagnosis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseNamesTest {
  @Test
  void treatsSuffixAndSpacingAsTheSameEnterprise() {
    assertTrue(EnterpriseNames.same("沈阳海龟医疗科技有限公司", " 沈阳海龟医疗科技公司 "));
    assertEquals(new EnterpriseNames.Match("相同", 100, "沈阳海龟医疗科技公司"), EnterpriseNames.best("沈阳海龟医疗科技有限公司", List.of("沈阳海龟医疗科技公司")));
  }

  @Test
  void flagsAboutEightyPercentSimilarNamesWithoutMergingThem() {
    EnterpriseNames.Match match = EnterpriseNames.best("沈阳海龟医疗有限公司", List.of("大连船舶重工集团有限公司", "沈阳海龟医疗科技有限公司"));

    assertEquals("相似", match.nameMatch());
    assertTrue(match.similarity() >= 80 && match.similarity() < 100);
    assertEquals("沈阳海龟医疗科技有限公司", match.matchedEnterpriseName());
    assertFalse(EnterpriseNames.same("沈阳海龟医疗有限公司", "沈阳海龟医疗科技有限公司"));
  }

  @Test
  void leavesClearlyDifferentNamesAsNew() {
    assertEquals(new EnterpriseNames.Match("新", 0, ""), EnterpriseNames.best("沈阳海龟医疗科技有限公司", List.of("大连船舶重工集团有限公司")));
  }
}
