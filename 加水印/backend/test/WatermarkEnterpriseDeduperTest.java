package com.weeklyplan.watermark;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermarkEnterpriseDeduperTest {
  @Test
  void treatsCityAndCountySuffixesAsTheSamePlace() {
    assertEquals(
        WatermarkPlaceText.identity("鞍山制药有限公司", "鞍山市", "铁西区"),
        WatermarkPlaceText.identity("鞍山制药有限公司", "鞍山", "铁西")
    );
    assertTrue(WatermarkPlaceText.samePlace("沈阳市", "大东区", "沈阳", "大东"));
  }

  @Test
  void mergesDuplicateRowsAndKeepsLongerPlaceAndFilledVisits() {
    LocalDateTime first = LocalDateTime.of(2026, 5, 13, 13, 56);
    LocalDateTime second = LocalDateTime.of(2026, 5, 22, 13, 32);
    WatermarkEnterprise older = WatermarkEnterprise.create(
        null, "鞍山制药有限公司", "鞍山", "铁西区", first, null, null
    );
    WatermarkEnterprise newer = WatermarkEnterprise.create(
        null, "鞍山制药有限公司", "鞍山市", "铁西", null, second, null
    );

    WatermarkEnterpriseDeduper.Plan plan = WatermarkEnterpriseDeduper.plan(List.of(older, newer));
    WatermarkEnterpriseDeduper.mergeIntoKeepers(plan);

    assertEquals(1, plan.keep().size());
    assertEquals(List.of(newer), plan.drop());
    assertEquals("鞍山市", older.getCity());
    assertEquals("铁西区", older.getCounty());
    assertEquals(first, older.getVisit1At());
    assertEquals(second, older.getVisit2At());
  }
}
