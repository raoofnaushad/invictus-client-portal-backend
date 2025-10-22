package com.asbitech.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomError {
    private HttpStatus httpStatusCode;
    private String errorCode;
    private String message;
    private Object[] args;
}
