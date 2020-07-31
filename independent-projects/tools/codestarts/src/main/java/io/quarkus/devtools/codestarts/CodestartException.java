package io.quarkus.devtools.codestarts;

public class CodestartException extends RuntimeException {

    public CodestartException(Throwable cause) {
        super(null, cause);
    }

    public CodestartException(String message) {
        super(message, null);
    }

}
