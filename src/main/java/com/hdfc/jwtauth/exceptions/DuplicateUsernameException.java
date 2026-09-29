package com.hdfc.jwtauth.exceptions;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String message) {
        super(message);
    }
}