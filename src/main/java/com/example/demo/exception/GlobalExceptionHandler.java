package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<?> handleValidation(Exception e) {
        String msg = "요청 값이 올바르지 않습니다.";
        if (e instanceof MethodArgumentNotValidException manv && manv.getBindingResult().getFieldError() != null) {
            msg = manv.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Map.of("error", "validation_error", "message", msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "bad_request", "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(Exception e) {
        log.error("Unexpected server error", e); // ✅ 에러 로그 전체 출력
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "server_error", "message", "서버 오류가 발생했습니다."));
    }
}
