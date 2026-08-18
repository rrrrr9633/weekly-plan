package com.weeklyplan.project;

import com.weeklyplan.company.Company;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTest {
  @Test
  void newProjectsAreVisibleAndCanBeHiddenIndependentlyOfTheirStatus() {
    Project project = Project.create(Company.create("COMPANY", "公司"), "项目", "PROJECT", null, null);

    assertFalse(project.isHidden());
    project.update(null, null, null, null, true);

    assertTrue(project.isHidden());
    assertEquals(ProjectStatus.ACTIVE, project.getStatus());
  }
}
