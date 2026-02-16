package org.link.linkvault.exception;

public class DuplicateUrlException extends RuntimeException {
    public DuplicateUrlException(String message) {
        super(message);
    }
}
