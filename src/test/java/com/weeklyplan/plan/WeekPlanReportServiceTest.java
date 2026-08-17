package com.weeklyplan.plan;

import com.weeklyplan.project.Project;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeekPlanReportServiceTest {
  @Test
  void exportsArchivedPlansAlongsideActivePlans() throws Exception {
    WeekPlanRepository repository = mock(WeekPlanRepository.class);
    WeekPlan activePlan = plan("进行中的计划", PlanStatus.ACTIVE, PlanWeekday.MONDAY);
    WeekPlan archivedPlan = plan("已归档的计划", PlanStatus.ARCHIVED, PlanWeekday.TUESDAY);
    when(repository.findParticipatingByUserAndWeek(7L, 2026, 32))
        .thenReturn(List.of(activePlan, archivedPlan));

    byte[] report = new WeekPlanReportService(repository).exportMine(7L, 2026, 32);

    verify(repository).findParticipatingByUserAndWeek(7L, 2026, 32);
    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(report))) {
      var sheet = workbook.getSheetAt(0);
      assertEquals("项目 / 进行中的计划 / 2026-08-03", sheet.getRow(4).getCell(2).getStringCellValue());
      assertEquals("项目 / 已归档的计划 / 2026-08-04", sheet.getRow(5).getCell(2).getStringCellValue());
      assertEquals("已完成", sheet.getRow(5).getCell(3).getStringCellValue());
    }
  }

  private WeekPlan plan(String content, PlanStatus status, PlanWeekday weekday) {
    Project project = mock(Project.class);
    when(project.getName()).thenReturn("项目");
    WeekPlan plan = mock(WeekPlan.class);
    when(plan.getProject()).thenReturn(project);
    when(plan.getContent()).thenReturn(content);
    when(plan.getStatus()).thenReturn(status);
    when(plan.getWeekday()).thenReturn(weekday);
    return plan;
  }
}
