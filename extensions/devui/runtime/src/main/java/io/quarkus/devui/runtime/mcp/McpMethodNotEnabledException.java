package io.quarkus.devui.runtime.mcp;

public class McpMethodNotEnabledException extends Exception {

    public McpMethodNotEnabledException() {
    }

    public McpMethodNotEnabledException(String message) {
        super(message);
    }

    public McpMethodNotEnabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public McpMethodNotEnabledException(Throwable cause) {
        super(cause);
    }

    public McpMethodNotEnabledException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
