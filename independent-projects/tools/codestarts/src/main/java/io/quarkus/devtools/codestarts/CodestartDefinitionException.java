package io.quarkus.devtools.codestarts;

public class CodestartDefinitionException extends RuntimeException {

    public CodestartDefinitionException(Throwable cause) {
        super(null, cause);
    }

    public CodestartDefinitionException(String message) {
        super(message, null);
    }

    public CodestartDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

}
