package com.weeklyplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeeklyPlanApplication {
  public static void main(String[] args) {
    SpringApplication.run(WeeklyPlanApplication.class, args);
  }
}
