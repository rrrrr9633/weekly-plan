package com.weeklyplan.reportpdf;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
class CoverWordComposer {
  private static final int MAX_WIDTH_EMU = 190 * 360000;
  private static final int MAX_HEIGHT_EMU = 277 * 360000;

  byte[] replaceFirstPage(byte[] docx, byte[] coverImage) {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
      insertCover(document, coverImage);
      dropOriginalFirstPage(document);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.write(output);
      return output.toByteArray();
    } catch (ResponseStatusException error) {
      throw error;
    } catch (Exception error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法把封面图写进 Word 第一页");
    }
  }

  private void insertCover(XWPFDocument document, byte[] coverImage) throws Exception {
    XWPFParagraph anchor = document.getParagraphs().isEmpty() ? document.createParagraph() : document.getParagraphs().get(0);
    XmlCursor cursor = anchor.getCTP().newCursor();
    XWPFParagraph cover = document.insertNewParagraph(cursor);
    cursor.dispose();
    cover.setAlignment(ParagraphAlignment.CENTER);
    cover.setSpacingBefore(0);
    cover.setSpacingAfter(0);
    XWPFRun run = cover.createRun();
    int[] size = fitSize(coverImage);
    try (ByteArrayInputStream picture = new ByteArrayInputStream(coverImage)) {
      run.addPicture(picture, pictureType(coverImage), "cover", size[0], size[1]);
    }
    run.addBreak(BreakType.PAGE);
  }

  private void dropOriginalFirstPage(XWPFDocument document) {
    List<IBodyElement> elements = document.getBodyElements();
    int end = -1;
    for (int index = 1; index < elements.size(); index++) {
      IBodyElement element = elements.get(index);
      if (element instanceof XWPFParagraph paragraph && hasPageBreak(paragraph)) {
        end = index;
        break;
      }
    }
    if (end < 1) return;
    for (int index = end; index >= 1; index--) {
      document.removeBodyElement(index);
    }
  }

  private boolean hasPageBreak(XWPFParagraph paragraph) {
    if (paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSectPr()) return true;
    for (XWPFRun run : paragraph.getRuns()) {
      for (CTBr br : run.getCTR().getBrList()) {
        if (br.getType() == STBrType.PAGE) return true;
      }
    }
    return false;
  }

  private int pictureType(byte[] coverImage) {
    if (coverImage.length >= 8
        && coverImage[0] == (byte) 0x89
        && coverImage[1] == 0x50
        && coverImage[2] == 0x4E
        && coverImage[3] == 0x47) {
      return XWPFDocument.PICTURE_TYPE_PNG;
    }
    return XWPFDocument.PICTURE_TYPE_JPEG;
  }

  private int[] fitSize(byte[] coverImage) {
    int width = MAX_WIDTH_EMU;
    int height = MAX_HEIGHT_EMU;
    try {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(coverImage));
      if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
        double aspect = (double) image.getWidth() / image.getHeight();
        double box = (double) MAX_WIDTH_EMU / MAX_HEIGHT_EMU;
        if (aspect > box) {
          width = MAX_WIDTH_EMU;
          height = (int) Math.round(MAX_WIDTH_EMU / aspect);
        } else {
          height = MAX_HEIGHT_EMU;
          width = (int) Math.round(MAX_HEIGHT_EMU * aspect);
        }
      }
    } catch (Exception ignored) {
    }
    return new int[] { Math.max(width, Units.toEMU(40)), Math.max(height, Units.toEMU(40)) };
  }
}
