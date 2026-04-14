package io.quarkus.devshell.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.quarkus.devui.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devui.runtime.comms.MessageType;

/**
 * In-process response writer for Dev Shell TUI.
 * Unlike WebSocket or HTTP writers, this directly passes responses
 * to the TUI via callbacks, avoiding any network overhead.
 */
public class DevShellResponseWriter implements JsonRpcResponseWriter {

    private final Consumer<String> responseHandler;
    private final CompletableFuture<String> responseFuture;
    private volatile boolean closed = false;

    /**
     * Creates a writer that delivers responses via callback.
     * Use this for streaming/subscription responses.
     *
     * @param responseHandler callback to receive response messages
     */
    public DevShellResponseWriter(Consumer<String> responseHandler) {
        this.responseHandler = responseHandler;
        this.responseFuture = null;
    }

    /**
     * Creates a writer that completes a future with the response.
     * Use this for request-response patterns where you need to wait for a result.
     *
     * @param responseFuture future to complete with the response
     * @param responseHandler optional callback for intermediate messages
     */
    public DevShellResponseWriter(CompletableFuture<String> responseFuture, Consumer<String> responseHandler) {
        this.responseFuture = responseFuture;
        this.responseHandler = responseHandler;
    }

    /**
     * Creates a writer that only completes a future.
     * Use this for simple request-response patterns.
     *
     * @param responseFuture future to complete with the response
     */
    public DevShellResponseWriter(CompletableFuture<String> responseFuture) {
        this.responseFuture = responseFuture;
        this.responseHandler = null;
    }

    @Override
    public void write(String message) {
        if (!closed) {
            if (responseHandler != null) {
                responseHandler.accept(message);
            }
            if (responseFuture != null && !responseFuture.isDone()) {
                responseFuture.complete(message);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public Object decorateObject(Object object, MessageType messageType) {
        // For Dev Shell, we return objects directly without any wrapper.
        // The TUI can work with the raw JSON-RPC response objects.
        return object;
    }
}
