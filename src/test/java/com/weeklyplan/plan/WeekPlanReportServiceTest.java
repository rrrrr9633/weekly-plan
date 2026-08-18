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
  void exportsArchivedAndHiddenProjectPlansAlongsideActivePlans() throws Exception {
    WeekPlanRepository repository = mock(WeekPlanRepository.class);
    WeekPlan activePlan = plan("进行中的计划", PlanStatus.ACTIVE, PlanWeekday.MONDAY, false);
    WeekPlan archivedPlan = plan("已归档的隐藏项目计划", PlanStatus.ARCHIVED, PlanWeekday.TUESDAY, true);
    when(repository.findParticipatingByUserAndWeek(7L, 2026, 32))
        .thenReturn(List.of(activePlan, archivedPlan));

    byte[] report = new WeekPlanReportService(repository).exportMine(7L, 2026, 32, null);

    verify(repository).findParticipatingByUserAndWeek(7L, 2026, 32);
    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(report))) {
      var sheet = workbook.getSheetAt(0);
      assertEquals("项目 / 进行中的计划 / 2026-08-03", sheet.getRow(4).getCell(2).getStringCellValue());
      assertEquals("项目 / 已归档的隐藏项目计划 / 2026-08-04", sheet.getRow(5).getCell(2).getStringCellValue());
      assertEquals("已完成", sheet.getRow(5).getCell(3).getStringCellValue());
    }
  }

  @Test
  void exportsOnlyPlansFromSelectedProjects() throws Exception {
    WeekPlanRepository repository = mock(WeekPlanRepository.class);
    WeekPlan selectedPlan = plan("指定项目计划", PlanStatus.ACTIVE, PlanWeekday.MONDAY);
    when(repository.findParticipatingByUserAndWeekAndProjectIdIn(7L, 2026, 32, List.of(9L)))
        .thenReturn(List.of(selectedPlan));

    byte[] report = new WeekPlanReportService(repository).exportMine(7L, 2026, 32, List.of(9L));

    verify(repository).findParticipatingByUserAndWeekAndProjectIdIn(7L, 2026, 32, List.of(9L));
    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(report))) {
      var sheet = workbook.getSheetAt(0);
      assertEquals("项目 / 指定项目计划 / 2026-08-03", sheet.getRow(4).getCell(2).getStringCellValue());
    }
  }

  private WeekPlan plan(String content, PlanStatus status, PlanWeekday weekday) {
    return plan(content, status, weekday, false);
  }

  private WeekPlan plan(String content, PlanStatus status, PlanWeekday weekday, boolean hiddenProject) {
    Project project = mock(Project.class);
    when(project.getName()).thenReturn("项目");
    when(project.isHidden()).thenReturn(hiddenProject);
    WeekPlan plan = mock(WeekPlan.class);
    when(plan.getProject()).thenReturn(project);
    when(plan.getContent()).thenReturn(content);
    when(plan.getStatus()).thenReturn(status);
    when(plan.getWeekday()).thenReturn(weekday);
    return plan;
  }
}
