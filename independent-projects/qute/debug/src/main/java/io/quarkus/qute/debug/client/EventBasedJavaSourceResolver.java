package io.quarkus.qute.debug.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaSourceResolver implementation for event-based clients (VS Code, etc.).
 * <p>
 * When the debugger needs to resolve a Qute template to a Java source, this
 * resolver sends an event to the client and waits asynchronously for the client
 * to respond.
 * </p>
 */
public class EventBasedJavaSourceResolver implements JavaSourceResolver {

    private final QuteDebugProtocolClient client;

    /**
     * Pending requests: maps request ID → CompletableFuture
     */
    private final Map<String, CompletableFuture<JavaSourceLocationResponse>> pending = new ConcurrentHashMap<>();

    public EventBasedJavaSourceResolver(QuteDebugProtocolClient client) {
        this.client = client;
    }

    /**
     * Resolves a Java source from a Qute template by sending an event to the
     * client.
     * <p>
     * Returns a CompletableFuture that will be completed when the client responds
     * via the {@code qute/onJavaSourceResolved} notification.
     * </p>
     */
    @Override
    public CompletableFuture<JavaSourceLocationResponse> resolveJavaSource(JavaSourceLocationArguments args) {
        String id = UUID.randomUUID().toString();
        CompletableFuture<JavaSourceLocationResponse> future = new CompletableFuture<>();
        pending.put(id, future);

        // Send event to the client
        JavaSourceLocationEventArguments request = new JavaSourceLocationEventArguments(id, args);
        client.onResolveJavaSource(request);

        return future;
    }

    /**
     * Handles the response from the client when a Java source has been resolved.
     * <p>
     * This should be called by the client event handler when it receives the
     * {@code qute/onJavaSourceResolved} notification.
     * </p>
     */
    public void handleResponse(JavaSourceLocationEventResponse response) {
        CompletableFuture<JavaSourceLocationResponse> future = pending.remove(response.getId());
        if (future != null) {
            future.complete(response.getResponse());
        }
    }

    /**
     * Optionally handles a cancellation from the server side.
     */
    public void cancel(String id) {
        CompletableFuture<JavaSourceLocationResponse> future = pending.remove(id);
        if (future != null) {
            future.cancel(true);
        }
    }
}
