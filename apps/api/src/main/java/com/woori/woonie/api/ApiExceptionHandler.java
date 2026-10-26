package com.woori.woonie.api;

import com.woori.woonie.api.ApiDtos.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBusinessException(RuntimeException exception) {
        return new ApiError("BUSINESS_RULE_VIOLATION", exception.getMessage());
    }
}
