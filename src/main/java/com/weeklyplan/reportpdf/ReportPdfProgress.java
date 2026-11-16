package com.weeklyplan.reportpdf;

@FunctionalInterface
interface ReportPdfProgress {
  void update(int percent, String stage, String message);
}
