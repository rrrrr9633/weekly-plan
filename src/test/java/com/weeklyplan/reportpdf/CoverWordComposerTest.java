package com.weeklyplan.reportpdf;

import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverWordComposerTest {
  private static final byte[] PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
  );

  @Test
  void putsCoverImageOnFirstPageAndDropsOriginalCover() throws Exception {
    byte[] source;
    try (XWPFDocument document = new XWPFDocument()) {
      document.createParagraph().createRun().setText("旧封面");
      XWPFParagraph pageBreak = document.createParagraph();
      pageBreak.createRun().addBreak(BreakType.PAGE);
      document.createParagraph().createRun().setText("正文还在");
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      document.write(bytes);
      source = bytes.toByteArray();
    }

    byte[] replaced = new CoverWordComposer().replaceFirstPage(source, PNG);

    try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(replaced))) {
      String text = document.getParagraphs().stream()
          .map(XWPFParagraph::getText)
          .reduce("", (left, right) -> left + right);
      assertTrue(text.contains("正文还在"));
      assertTrue(!text.contains("旧封面"));
      assertTrue(document.getAllPictures().size() >= 1);
    }
  }
}
