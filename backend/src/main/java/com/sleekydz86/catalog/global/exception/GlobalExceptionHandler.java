package com.sleekydz86.catalog.global.exception;

import com.sleekydz86.catalog.global.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("리소스를 찾을 수 없음 path={} code={} message={}",
                request.getRequestURI(), exception.code(), exception.getMessage());
        return build(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ResourceConflictException exception,
            HttpServletRequest request
    ) {
        log.warn("리소스 충돌 path={} code={} message={}",
                request.getRequestURI(), exception.code(), exception.getMessage());
        return build(HttpStatus.CONFLICT, exception, request);
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ApiErrorResponse> handleInfrastructure(
            InfrastructureException exception,
            HttpServletRequest request
    ) {
        log.error("인프라/외부 연동 오류 path={} code={} message={}",
                request.getRequestURI(), exception.code(), exception.getMessage(), exception);
        return build(resolveStatus(exception.code()), exception, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = resolveStatus(exception.code());
        if (status.is5xxServerError()) {
            log.error("업무 예외(서버) path={} code={} message={}",
                    request.getRequestURI(), exception.code(), exception.getMessage(), exception);
        } else {
            log.warn("업무 예외 path={} code={} message={}",
                    request.getRequestURI(), exception.code(), exception.getMessage());
        }
        return build(status, exception, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        log.warn("잘못된 요청 path={} message={}", request.getRequestURI(), exception.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, safeMessage(exception), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        log.error("잘못된 상태/인프라 오류 path={} message={}",
                request.getRequestURI(), exception.getMessage(), exception);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "요청을 처리하는 중 오류가 발생했습니다.",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("입력값 검증 실패 path={} message={}", request.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternal(Exception exception, HttpServletRequest request) {
        log.error("처리되지 않은 오류 발생 path={}", request.getRequestURI(), exception);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다.",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            BusinessException exception,
            HttpServletRequest request
    ) {
        return build(status, exception.code(), exception.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                code.name()
        );
        return ResponseEntity.status(status).body(body);
    }

    private static HttpStatus resolveStatus(ErrorCode code) {
        return switch (code) {
            case RESOURCE_NOT_FOUND,
                 CONNECTION_NOT_FOUND,
                 METADATA_SET_NOT_FOUND,
                 CATEGORY_NOT_FOUND,
                 EXTRACT_REQUEST_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESOURCE_CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED, INVALID_USER_STATUS, UNSUPPORTED_DDL_MAPPING, CONNECTION_UNHEALTHY ->
                    HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case EXTERNAL_SERVICE_FAILED -> HttpStatus.BAD_GATEWAY;
            case MIGRATION_FAILED,
                 METADATA_SYNC_FAILED,
                 EXTRACT_FAILED,
                 INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "요청 값이 올바르지 않습니다." : message;
    }
}
