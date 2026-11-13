package com.weeklyplan.reportpdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;

@Component
class CoverPdfComposer {
  byte[] replaceFirstPage(byte[] wordPdf, byte[] coverImage) {
    try (PDDocument source = Loader.loadPDF(wordPdf); PDDocument output = new PDDocument()) {
      if (source.getNumberOfPages() == 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word 转出的 PDF 是空的");
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
      for (int index = 1; index < source.getNumberOfPages(); index++) {
        output.importPage(source.getPage(index));
      }
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      output.save(bytes);
      return bytes.toByteArray();
    } catch (ResponseStatusException error) {
      throw error;
    } catch (Exception error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法把封面图和 Word 页合并成 PDF");
    }
  }
}
