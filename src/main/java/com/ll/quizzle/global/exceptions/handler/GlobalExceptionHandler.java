package com.ll.quizzle.global.exceptions.handler;

import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.exceptions.ServiceException;
import com.ll.quizzle.global.response.RsData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<RsData<?>> handle(ServiceException ex) {
        log.error("ServiceException: {}", ex.getMessage());
        
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus();
        String message = ex.getMsg() != null ? ex.getMsg() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage();

        return ResponseEntity
                .status(status)
                .body(new RsData<>(status, message, null));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RsData<?>> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage());
        
        if (ex.getMessage() != null && ex.getMessage().contains("Session was invalidated")) {
            log.warn("세션 무효화 오류 발생 - 클라이언트에게 401 응답 전송");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해주세요.", null));
        }
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<?>> handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder errorMessages = new StringBuilder();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorMessages.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage())
                    .append("\n");
        }
        
        String errorMsg = errorMessages.toString();
        log.debug("유효성 검증 오류: {}", errorMsg);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>(HttpStatus.BAD_REQUEST, errorMsg, null));
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String errorMessage = "요청 본문을 읽을 수 없습니다. 유효한 JSON 형식인지 확인하세요.";
        Throwable cause = ex.getCause();
        
        if (cause != null) {
            errorMessage += " 원인: " + cause.getMessage();
        }
        
        log.debug("잘못된 요청 본문: {}", errorMessage, ex);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>(HttpStatus.BAD_REQUEST, errorMessage, null));
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RsData<?>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String errorMessage = String.format("파라미터 '%s'의 값이 올바르지 않습니다. 예상 타입: %s", 
                paramName, ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "알 수 없음");
                
        log.debug("타입 불일치 오류: {} - 입력값: '{}'", errorMessage, ex.getValue());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>(HttpStatus.BAD_REQUEST, errorMessage, null));
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RsData<?>> handleMissingParameterException(MissingServletRequestParameterException ex) {
        String errorMessage = String.format("필수 파라미터 '%s'가 누락되었습니다.", ex.getParameterName());
        
        log.debug("파라미터 누락: {}", errorMessage);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>(HttpStatus.BAD_REQUEST, errorMessage, null));
    }
    
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<RsData<?>> handleDateTimeParseException(DateTimeParseException ex) {
        String errorMessage = String.format("날짜/시간 형식이 올바르지 않습니다: '%s'", ex.getParsedString());
        
        log.debug("날짜 형식 오류: {}", errorMessage);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>(HttpStatus.BAD_REQUEST, errorMessage, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<?>> handleGenericException(Exception ex) {
        log.error("예상치 못한 오류가 발생했습니다:", ex);
        
        String errorMsg = "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.";
        
        if (ex.getMessage() != null) {
            errorMsg += " (오류 유형: " + ex.getClass().getSimpleName() + ")";
        }

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(new RsData<>(
                        ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                        errorMsg,
                        null
                ));
    }
}