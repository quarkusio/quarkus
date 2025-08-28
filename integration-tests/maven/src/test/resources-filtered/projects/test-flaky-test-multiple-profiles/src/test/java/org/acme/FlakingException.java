package org.acme;

public class FlakingException extends Exception {
    public FlakingException(String message) {
        super(message);
    }
}