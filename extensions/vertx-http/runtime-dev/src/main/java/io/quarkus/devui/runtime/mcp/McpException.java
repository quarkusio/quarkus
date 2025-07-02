package io.quarkus.devui.runtime.mcp;

class McpException extends RuntimeException {

    private static final long serialVersionUID = 3142589829095593984L;

    private final int jsonRpcError;

    McpException(String message, Throwable cause, int jsonRpcError) {
        super(message, cause);
        this.jsonRpcError = jsonRpcError;
    }

    McpException(String message, int jsonRpcError) {
        super(message);
        this.jsonRpcError = jsonRpcError;
    }

    int getJsonRpcError() {
        return jsonRpcError;
    }

}
