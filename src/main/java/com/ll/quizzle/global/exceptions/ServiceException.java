package com.ll.quizzle.global.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String msg;

    public ServiceException(HttpStatus resultCode, String msg) {
        super(resultCode + " : " + msg);
        this.httpStatus = resultCode;
        this.msg = msg;
    }

    public ServiceException(HttpStatus resultCode, String msg, Throwable cause) {
        super(resultCode + " : " + msg, cause);
        this.httpStatus = resultCode;
        this.msg = msg;
    }
}
