package com.github.oahnus.proxyserver.exceptions;

/**
 * Created by oahnus on 2020-04-27
 * 10:32.
 */
public class AuthException extends RuntimeException {
    public AuthException() {
        super();
    }

    public AuthException(String message) {
        super(message);
    }
}
