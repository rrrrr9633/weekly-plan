package com.weeklyplan.reportpdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
class LibreOfficePdfConverter {
  private final String configuredBin;

  LibreOfficePdfConverter(@Value("${app.libreoffice-bin:}") String configuredBin) {
    this.configuredBin = configuredBin == null ? "" : configuredBin.trim();
  }

  byte[] convertWord(Path input, Path workDir) {
    return convertWord(input, workDir, null);
  }

  byte[] convertWord(Path input, Path workDir, ReportPdfProgress progress) {
    Path pdf = convertToFile(input, workDir, "pdf:writer_pdf_Export", ".pdf", "Word 转 PDF", progress);
    try {
      return java.nio.file.Files.readAllBytes(pdf);
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取转出的 PDF");
    }
  }

  Path convertWordToFile(Path input, Path workDir, ReportPdfProgress progress) {
    return convertToFile(input, workDir, "pdf:writer_pdf_Export", ".pdf", "Word 转 PDF", progress);
  }

  byte[] convertToDocx(Path input, Path workDir) {
    return convertToDocx(input, workDir, null);
  }

  byte[] convertToDocx(Path input, Path workDir, ReportPdfProgress progress) {
    String name = input.getFileName().toString().toLowerCase();
    try {
      if (name.endsWith(".docx")) return Files.readAllBytes(input);
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取 Word 文件");
    }
    Path docx = convertToFile(input, workDir, "docx", ".docx", "旧版 Word 转 docx", progress);
    try {
      return Files.readAllBytes(docx);
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取转换后的 Word");
    }
  }

  private Path convertToFile(
      Path input,
      Path workDir,
      String filter,
      String extension,
      String label,
      ReportPdfProgress progress
  ) {
    String bin = resolveBin();
    Path profile = workDir.resolve("lo-profile");
    long timeoutSeconds = timeoutSeconds(input);
    try {
      Files.createDirectories(profile);
      tick(progress, 28, "converting", label + "已开始。约 " + sizeMb(input) + "MB，可能需要几分钟");
      ProcessBuilder builder = new ProcessBuilder(
          bin,
          "--headless",
          "--nologo",
          "--nofirststartwizard",
          "--norestore",
          "--nolockcheck",
          "-env:UserInstallation=" + profile.toUri(),
          "--convert-to", filter,
          "--outdir", workDir.toAbsolutePath().toString(),
          input.toAbsolutePath().toString()
      );
      builder.redirectErrorStream(true);
      Path logFile = workDir.resolve("lo.log");
      builder.redirectOutput(logFile.toFile());
      Process process = builder.start();
      long started = System.nanoTime();
      long timeoutNs = TimeUnit.SECONDS.toNanos(timeoutSeconds);
      while (process.isAlive()) {
        long elapsedNs = System.nanoTime() - started;
        if (elapsedNs > timeoutNs) {
          killTree(process);
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              label + "超时（已等 " + timeoutSeconds + " 秒）。请把 Word 另存为 PDF 后再上传换封面，或减少图片体积。"
          );
        }
        long elapsedSec = TimeUnit.NANOSECONDS.toSeconds(elapsedNs);
        int percent = 28 + (int) Math.min(50, (elapsedSec * 50) / Math.max(timeoutSeconds, 1));
        tick(progress, percent, "converting",
            label + "进行中 " + elapsedSec + " / " + timeoutSeconds + " 秒（大 Word 会在这一步最久）");
        process.waitFor(2, TimeUnit.SECONDS);
      }
      String log = Files.exists(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : "";
      if (process.exitValue() != 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "失败。" + brief(log));
      }
      Path output = workDir.resolve(replaceExtension(input.getFileName().toString(), extension));
      if (!Files.exists(output)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "没有生成文件。" + brief(log));
      }
      tick(progress, 80, "converting", label + "完成");
      return output;
    } catch (ResponseStatusException error) {
      throw error;
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "被中断");
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法调用 LibreOffice 转换 Word");
    }
  }

  private long timeoutSeconds(Path input) {
    long mb = Math.max(1, sizeMb(input));
    return Math.min(720, Math.max(180, mb * 12));
  }

  private long sizeMb(Path input) {
    try {
      return Math.max(1, Files.size(input) / (1024 * 1024));
    } catch (IOException error) {
      return 1;
    }
  }

  private void killTree(Process process) {
    process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }

  private void tick(ReportPdfProgress progress, int percent, String stage, String message) {
    if (progress != null) progress.update(percent, stage, message);
  }

  private String resolveBin() {
    for (String candidate : List.of(
        configuredBin,
        "soffice",
        "libreoffice",
        "/usr/bin/soffice",
        "/usr/bin/libreoffice",
        "/usr/lib/libreoffice/program/soffice",
        "/Applications/LibreOffice.app/Contents/MacOS/soffice"
    )) {
      if (candidate == null || candidate.isBlank()) continue;
      Path path = Path.of(candidate);
      if (path.isAbsolute() && Files.isExecutable(path)) return path.toString();
      if (!path.isAbsolute() && onPath(candidate)) return candidate;
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "服务器未安装 LibreOffice，无法把 Word 转成 PDF。请安装 libreoffice，或设置 LIBREOFFICE_BIN 指向 soffice。"
    );
  }

  private boolean onPath(String command) {
    String path = System.getenv("PATH");
    if (path == null) return false;
    for (String dir : path.split(java.io.File.pathSeparator)) {
      Path bin = Path.of(dir, command);
      if (Files.isExecutable(bin)) return true;
    }
    return false;
  }

  private String replaceExtension(String name, String extension) {
    int dot = name.lastIndexOf('.');
    String stem = dot > 0 ? name.substring(0, dot) : name;
    return stem + extension;
  }

  private String brief(String log) {
    String text = log == null ? "" : log.replaceAll("\\s+", " ").trim();
    if (text.isBlank()) return "";
    return text.length() > 180 ? text.substring(0, 180) : text;
  }
}
