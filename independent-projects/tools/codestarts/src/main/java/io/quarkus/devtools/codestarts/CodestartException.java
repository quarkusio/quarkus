package io.quarkus.devtools.codestarts;

public class CodestartException extends RuntimeException {

    public CodestartException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodestartException(Throwable cause) {
        super(null, cause);
    }

    public CodestartException(String message) {
        super(message, null);
    }

}
