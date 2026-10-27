package com.woori.wonnie.api;

import com.woori.wonnie.api.ApiDtos.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(com.woori.wonnie.assistant.AssistantUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError assistantUnavailable(com.woori.wonnie.assistant.AssistantUnavailableException exception) {
        return new ApiError("ASSISTANT_UNAVAILABLE", exception.getMessage());
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
}
