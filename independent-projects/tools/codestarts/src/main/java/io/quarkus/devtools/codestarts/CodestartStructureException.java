package io.quarkus.devtools.codestarts;

public class CodestartStructureException extends RuntimeException {

    public CodestartStructureException(Throwable cause) {
        super(null, cause);
    }

    public CodestartStructureException(String message) {
        super(message, null);
    }

    public CodestartStructureException(String message, Throwable cause) {
        super(message, cause);
    }

}
