package com.weeklyplan.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException error) {
    var fieldError = error.getBindingResult().getFieldError();
    String message = switch (fieldError == null ? "" : fieldError.getField()) {
      case "password" -> "密码长度必须为 8–72 位";
      case "username" -> "用户名不能为空且不超过 64 位";
      default -> validationMessage(fieldError);
    };
    return ResponseEntity.badRequest().body(new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleTooLarge(MaxUploadSizeExceededException error) {
    return ResponseEntity.badRequest().body(new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), "文件过大，请上传 20MB 以内的表格"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException error) {
    int status = error.getStatusCode().value();
    String message = error.getReason() == null ? "请求失败" : error.getReason();
    return ResponseEntity.status(error.getStatusCode()).body(new ApiErrorResponse(status, message));
  }

  public record ApiErrorResponse(int status, String message) {}

  private String validationMessage(org.springframework.validation.FieldError fieldError) {
    if (fieldError == null) return "请求参数不符合要求";
    String raw = fieldError.getField() == null ? "" : fieldError.getField();
    int dot = raw.lastIndexOf('.');
    String name = dot >= 0 ? raw.substring(dot + 1) : raw;
    String label = switch (name) {
      case "enterpriseName" -> "企业名称";
      case "county" -> "区县";
      case "diagnosisTime" -> "诊断时间";
      case "workDate" -> "日期";
      case "diagnosisRound" -> "轮次";
      case "enterpriseContact" -> "企业联系人";
      case "enterpriseContactPhone" -> "企业联系方式";
      case "participantIds", "participantNames" -> "参与人员";
      default -> "提交内容";
    };
    String code = fieldError.getCode() == null ? "" : fieldError.getCode();
    String why = switch (code) {
      case "NotBlank", "NotNull", "NotEmpty" -> "不能为空";
      case "Size" -> "超出长度限制";
      default -> "不符合要求";
    };
    return label + why;
  }
}
