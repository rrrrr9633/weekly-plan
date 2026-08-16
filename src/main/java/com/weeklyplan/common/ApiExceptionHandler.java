package com.weeklyplan.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException error) {
    var fieldError = error.getBindingResult().getFieldError();
    String message = switch (fieldError == null ? "" : fieldError.getField()) {
      case "password" -> "密码长度必须为 8–72 位";
      case "username" -> "用户名不能为空且不超过 64 位";
      default -> "请求参数不符合要求";
    };
    return ResponseEntity.badRequest().body(new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException error) {
    int status = error.getStatusCode().value();
    String message = error.getReason() == null ? "请求失败" : error.getReason();
    return ResponseEntity.status(error.getStatusCode()).body(new ApiErrorResponse(status, message));
  }

  public record ApiErrorResponse(int status, String message) {}
}
