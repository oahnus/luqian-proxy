package com.github.oahnus.proxyserver.config;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.luqiancommon.enums.web.RespCode;
import com.github.oahnus.proxyserver.exceptions.AuthException;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Created by oahnus on 2020-04-28
 * 18:52.
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(ServiceException.class)
    public RespData serviceExceptionHandler(RuntimeException e) {
        Throwable cause = e.getCause();
//        e.printStackTrace();
        if (cause instanceof ServiceException){
            ServiceException serviceException = (ServiceException) cause;
            return RespData.error(RespCode.SERVICE_ERROR, serviceException.getMessage());
        }
        if (cause instanceof AuthException){
            AuthException authException = (AuthException) cause;
            return RespData.error(RespCode.NO_AUTH, authException.getMessage());
        }
        return RespData.error(RespCode.INNER_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public RespData nullPointerExceptionHandler(NullPointerException e) {
//        e.printStackTrace();
        StackTraceElement element = e.getStackTrace()[0];
        log.error("File: {}, Line: {}, Method: {}, Message: {}",
                element.getFileName(),
                element.getLineNumber(),
                element.getMethodName(), e.getMessage());
        return RespData.error(RespCode.INNER_SERVER_ERROR, e.getMessage());
    }
}
