package io.quarkus.bootstrap.prebuild;

/**
 * PreBuildStep exception that translates to MojoFailureException (instead of MojoExecutionException)
 */
public class CodeGenFailureException extends CodeGenException {
    public CodeGenFailureException(String message) {
        super(message);
    }

    public CodeGenFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodeGenFailureException(Throwable cause) {
        super(cause);
    }

    public CodeGenFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
