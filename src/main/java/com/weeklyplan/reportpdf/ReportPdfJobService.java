package com.weeklyplan.reportpdf;

import com.weeklyplan.tenant.TenantAccessService;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ReportPdfJobService {
  private final ReportPdfService reports;
  private final TenantAccessService tenant;
  private final Map<String, Job> jobs = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor(thread -> {
    Thread worker = new Thread(thread, "report-pdf-job");
    worker.setDaemon(true);
    return worker;
  });

  public ReportPdfJobService(ReportPdfService reports, TenantAccessService tenant) {
    this.reports = reports;
    this.tenant = tenant;
  }

  public record Status(
      String id,
      String status,
      int progress,
      String stage,
      String message,
      String filename
  ) {}

  public Status start(MultipartFile report, MultipartFile cover, String format) {
    purgeExpired();
    Long companyId = tenant.currentCompany().getId();
    Job job = new Job(UUID.randomUUID().toString(), companyId);
    try {
      job.report = copy(report);
      job.cover = copy(cover);
      job.format = format;
      job.reportName = report.getOriginalFilename() == null ? "报告" : report.getOriginalFilename();
      job.coverName = cover.getOriginalFilename() == null ? "cover.jpg" : cover.getOriginalFilename();
    } catch (IOException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法保存上传的文件");
    }
    job.update(20, "queued", "文件已收到，正在排队生成");
    jobs.put(job.id, job);
    executor.execute(() -> run(job));
    return job.view();
  }

  public Status status(String id) {
    return require(id).view();
  }

  public JobFile file(String id) {
    Job job = require(id);
    if (!"done".equals(job.status) || job.result == null || !Files.exists(job.result)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, job.error == null ? "还没有生成完成" : job.error);
    }
    return new JobFile(new FileSystemResource(job.result), job.filename, job.mediaType);
  }

  public record JobFile(Resource resource, String filename, MediaType mediaType) {}

  private void run(Job job) {
    job.status = "running";
    job.update(16, "saving", "开始处理 " + job.reportName);
    try {
      MultipartFile report = job.report;
      MultipartFile cover = job.cover;
      ReportPdfService.Export export = reports.convert(report, cover, job.format, job::update);
      job.filename = export.filename();
      job.mediaType = export.mediaType();
      Path result = Files.createTempFile("report-result-", job.filename.endsWith(".docx") ? ".docx" : ".pdf");
      Files.write(result, export.bytes());
      job.result = result;
      job.status = "done";
      job.update(100, "done", "生成完成，开始下载");
    } catch (ResponseStatusException error) {
      job.status = "failed";
      job.error = error.getReason() == null ? "生成失败" : error.getReason();
      job.update(job.progress, "failed", job.error);
    } catch (Exception error) {
      job.status = "failed";
      job.error = "生成失败，请稍后重试";
      job.update(job.progress, "failed", job.error);
    } finally {
      if (job.report != null) deleteQuietly(job.report.path);
      if (job.cover != null) deleteQuietly(job.cover.path);
      job.report = null;
      job.cover = null;
    }
  }

  private Job require(String id) {
    Job job = jobs.get(id);
    if (job == null || !job.companyId.equals(tenant.currentCompany().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到这次生成任务");
    }
    return job;
  }

  private void purgeExpired() {
    Instant cutoff = Instant.now().minusSeconds(TimeUnit.HOURS.toSeconds(2));
    jobs.values().removeIf(job -> {
      if (job.created.isBefore(cutoff)) {
        deleteQuietly(job.result);
        return true;
      }
      return false;
    });
  }

  private StoredFile copy(MultipartFile file) throws IOException {
    String name = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
    Path path = Files.createTempFile("report-upload-", "-" + name.replaceAll("[^\\w.\\u4e00-\\u9fff-]+", "_"));
    try (var input = file.getInputStream()) {
      Files.copy(input, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    return new StoredFile(name, path, file.getContentType());
  }

  private void deleteQuietly(Path path) {
    if (path == null) return;
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
    }
  }

  @PreDestroy
  void shutdown() {
    executor.shutdownNow();
  }

  static final class Job {
    final String id;
    final Long companyId;
    final Instant created = Instant.now();
    volatile String status = "queued";
    volatile int progress = 5;
    volatile String stage = "queued";
    volatile String message = "排队中";
    volatile String error;
    volatile String filename;
    volatile MediaType mediaType;
    volatile Path result;
    volatile String format;
    volatile String reportName;
    volatile String coverName;
    volatile StoredFile report;
    volatile StoredFile cover;

    Job(String id, Long companyId) {
      this.id = id;
      this.companyId = companyId;
    }

    void update(int percent, String stage, String message) {
      this.progress = Math.max(this.progress, Math.min(100, percent));
      this.stage = stage;
      this.message = message;
    }

    Status view() {
      return new Status(id, status, progress, stage, message, filename);
    }
  }

  static final class StoredFile implements MultipartFile {
    private final String name;
    private final Path path;
    private final String contentType;

    StoredFile(String name, Path path, String contentType) {
      this.name = name;
      this.path = path;
      this.contentType = contentType;
    }

    @Override public String getName() { return "file"; }
    @Override public String getOriginalFilename() { return name; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() {
      try {
        return Files.size(path) == 0;
      } catch (IOException error) {
        return true;
      }
    }
    @Override public long getSize() {
      try {
        return Files.size(path);
      } catch (IOException error) {
        return 0;
      }
    }
    @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
    @Override public java.io.InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
    @Override public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
      Files.copy(path, dest.toPath());
    }
  }
}
