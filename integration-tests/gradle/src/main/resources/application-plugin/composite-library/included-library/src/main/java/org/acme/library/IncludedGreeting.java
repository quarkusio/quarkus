package org.acme.library;

public final class IncludedGreeting {

    private IncludedGreeting() {
    }

    public static String message() {
        return "hello from the included library";
    }
}
