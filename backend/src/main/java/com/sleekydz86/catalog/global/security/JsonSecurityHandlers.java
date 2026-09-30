package com.sleekydz86.catalog.global.security;

import com.sleekydz86.catalog.global.api.ApiErrorResponse;
import com.sleekydz86.catalog.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;

public final class JsonSecurityHandlers {

    private JsonSecurityHandlers() {
    }

    public static AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) ->
                write(response, request, objectMapper, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
    }

    public static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) ->
                write(response, request, objectMapper, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "접근 권한이 없습니다.");
    }

    private static void write(
            HttpServletResponse response,
            HttpServletRequest request,
            ObjectMapper objectMapper,
            HttpStatus status,
            ErrorCode code,
            String message
    ) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                code.name()
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
