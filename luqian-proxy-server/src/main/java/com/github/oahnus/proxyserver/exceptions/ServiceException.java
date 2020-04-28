package com.github.oahnus.proxyserver.exceptions;

/**
 * Created by oahnus on 2020-04-23
 * 7:22.
 */
public class ServiceException extends RuntimeException {
    public ServiceException() {
        super();
    }

    public ServiceException(String message) {
        super(message);
    }
}
