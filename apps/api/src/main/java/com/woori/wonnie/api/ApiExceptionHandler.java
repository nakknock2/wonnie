package com.woori.wonnie.api;

import com.woori.wonnie.api.ApiDtos.ApiError;
import com.woori.wonnie.assistant.AssistantUnavailableException;
import com.woori.wonnie.assistant.AssistantRequestConflictException;
import com.woori.wonnie.assistant.AssistantRequestNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AssistantUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError assistantUnavailable(AssistantUnavailableException exception, HttpServletRequest request) {
        log.error("ASSISTANT_UNAVAILABLE method={} path={} stage={} cause={}",
                request.getMethod(), request.getRequestURI(), exception.getStage(), diagnostic(exception));
        return new ApiError("ASSISTANT_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(AssistantRequestConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError assistantRequestConflict(AssistantRequestConflictException exception) {
        return new ApiError("REQUEST_ID_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(AssistantRequestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError assistantRequestNotFound(AssistantRequestNotFoundException exception) {
        return new ApiError("REQUEST_NOT_FOUND", exception.getMessage());
    }

    // Provider errors can include credentials. Log the cause chain, without request/chat bodies or raw Throwables.
    private static String diagnostic(Throwable failure) {
        var detail = new StringBuilder();
        var cause = failure.getCause();
        for (int depth = 0; cause != null && depth < 8; depth++) {
            if (!detail.isEmpty()) detail.append(" -> ");
            detail.append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }
        var redacted = detail.toString()
                .replaceAll("sk-[A-Za-z0-9_.*-]+", "[REDACTED]")
                .replaceAll("(?i)(Bearer\\s+)[^\\s\"',;}]+", "$1[REDACTED]")
                .replaceAll("(?i)((?:api[_-]?key|authorization)\"?\\s*[:=]\\s*\"?)[^\\s\",;}]+", "$1[REDACTED]")
                .replace('\n', ' ').replace('\r', ' ');
        return redacted.length() > 4000 ? redacted.substring(0, 4000) + " [truncated]" : redacted;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidRequest(MethodArgumentNotValidException exception) {
        return new ApiError("INVALID_REQUEST", "입력값이 없거나 허용 범위를 벗어났어요. 내용을 확인해 주세요.");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBusinessException(RuntimeException exception) {
        return new ApiError("BUSINESS_RULE_VIOLATION", exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        var reason = exception.getReason() == null ? "요청을 처리하지 못했어요." : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiError("CHECKOUT_REQUEST_REJECTED", reason));
    }
}
