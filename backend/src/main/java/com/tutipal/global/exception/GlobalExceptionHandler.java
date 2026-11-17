package com.tutipal.global.exception;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.tutipal.global.ai.AiUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApi(ApiException e) {
        return body(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        var error = e.getBindingResult().getFieldErrors().stream().findFirst();
        String message = error.map(f -> f.getField() + ": " + f.getDefaultMessage()).orElse("잘못된 요청");
        return body(HttpStatus.BAD_REQUEST, message);
    }

    /** AI 서버(FastAPI/Ollama)가 꺼졌거나 오류: 화면이 "잠시 후 다시" 로 안내할 수 있게 503 + 문구 (위젯은 503 이면 조용히 다음 차례) */
    @ExceptionHandler(AiUnavailableException.class)
    ResponseEntity<Map<String, Object>> handleAi(AiUnavailableException e) {
        return body(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    /** 같은 요청이 동시에 두 번 와서 유일 제약에 걸린 경우 (예: 세션 참여 버튼 두 번) → 500 대신 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException e) {
        return body(HttpStatus.CONFLICT, "이미 처리된 요청이에요. 새로고침해 주세요");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
    }
}
