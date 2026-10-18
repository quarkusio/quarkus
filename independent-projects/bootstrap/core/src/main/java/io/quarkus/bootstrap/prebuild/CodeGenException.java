package io.quarkus.bootstrap.prebuild;

/**
 * An exception thrown by the CodeGenProvider's.
 * Translates to maven's MojoExecutionException
 */
public class CodeGenException extends Exception {
    public CodeGenException(String message) {
        super(message);
    }

    public CodeGenException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodeGenException(Throwable cause) {
        super(cause);
    }

    public CodeGenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
