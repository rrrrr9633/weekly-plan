package com.weeklyplan.reportpdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

@Component
class CoverPdfComposer {
  byte[] replaceFirstPage(byte[] reportPdf, byte[] coverImage) {
    return replaceFirstPage(reportPdf, coverImage, null);
  }

  byte[] replaceFirstPage(byte[] reportPdf, byte[] coverImage, ReportPdfProgress progress) {
    tick(progress, 82, "composing", "正在打开报告 PDF");
    try (PDDocument source = Loader.loadPDF(reportPdf); PDDocument output = new PDDocument()) {
      if (source.getNumberOfPages() == 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这份 PDF 是空的");
      }
      PDPage cover = new PDPage(PDRectangle.A4);
      output.addPage(cover);
      PDImageXObject image;
      try {
        image = PDImageXObject.createFromByteArray(output, coverImage, "cover");
      } catch (Exception error) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面图无法读取，请用 jpg 或 png");
      }
      PDRectangle box = cover.getMediaBox();
      float scale = Math.min(box.getWidth() / image.getWidth(), box.getHeight() / image.getHeight());
      float width = image.getWidth() * scale;
      float height = image.getHeight() * scale;
      float x = (box.getWidth() - width) / 2;
      float y = (box.getHeight() - height) / 2;
      try (PDPageContentStream stream = new PDPageContentStream(output, cover)) {
        stream.drawImage(image, x, y, width, height);
      }
      tick(progress, 86, "composing", "正在合并报告页并保留中文字体");
      // LibreOffice 的中文字体常挂在 Pages 父节点上，逐页 importPage 会丢掉字体，汉字变成方框。
      new PDFMergerUtility().appendDocument(output, source);
      if (output.getNumberOfPages() > 1) output.removePage(1);
      tick(progress, 95, "composing", "正在写出 PDF");
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      output.save(bytes);
      return bytes.toByteArray();
    } catch (ResponseStatusException error) {
      throw error;
    } catch (Exception error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法把封面图和报告页合并成 PDF");
    }
  }

  byte[] replaceFirstPage(Path reportPdf, byte[] coverImage, ReportPdfProgress progress) {
    try {
      tick(progress, 82, "composing", "正在读取报告 PDF");
      return replaceFirstPage(java.nio.file.Files.readAllBytes(reportPdf), coverImage, progress);
    } catch (ResponseStatusException error) {
      throw error;
    } catch (Exception error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取转换后的 PDF");
    }
  }

  private void tick(ReportPdfProgress progress, int percent, String stage, String message) {
    if (progress != null) progress.update(percent, stage, message);
  }
}
