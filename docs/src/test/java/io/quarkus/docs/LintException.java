package io.quarkus.docs;

public class LintException extends RuntimeException {
    // Exception that has no stacktrace
    public LintException(String fileName) {
        super("Found errors in document metadata. See test output or " + fileName + " for details.",
                null, false, false);
    }
}