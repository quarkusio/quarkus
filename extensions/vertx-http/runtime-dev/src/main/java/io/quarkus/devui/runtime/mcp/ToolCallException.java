package io.quarkus.devui.runtime.mcp;

/**
 * Indicates a business logic error in a {@link Tool} method.
 */
public class ToolCallException extends RuntimeException {

    private static final long serialVersionUID = 6214164159077697693L;

    public ToolCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public ToolCallException(String message) {
        super(message);
    }

    public ToolCallException(Throwable cause) {
        super(cause);
    }

}
