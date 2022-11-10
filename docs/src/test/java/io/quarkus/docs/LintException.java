package io.quarkus.docs;

public class LintException extends RuntimeException {
    // Exception that has no stacktrace
    public LintException(String fileName) {
        super("Metadata errors.\nSee test output or " + fileName + " for details.",
                null, false, false);
    }
}