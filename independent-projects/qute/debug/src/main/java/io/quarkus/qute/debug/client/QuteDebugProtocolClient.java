package io.quarkus.qute.debug.client;

import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/**
 * Combined Debug Protocol client interface for Qute templates.
 *
 * <p>
 * Extends the standard {@link IDebugProtocolClient} to include the
 * {@link JavaSourceResolver} functionality for resolving Java sources
 * referenced from Qute templates via {@code qute-java://} URIs.
 * </p>
 *
 * <p>
 * Event-based clients (like VS Code) can override the notification method
 * {@link #onResolveJavaSource(JavaSourceLocationEventArguments)}
 * to asynchronously resolve Java sources for template breakpoints.
 * </p>
 *
 * <p>
 * Blocking clients (like IntelliJ, Eclipse IDE) can simply implement
 * {@link JavaSourceResolver} and resolve the source synchronously.
 * </p>
 */
public interface QuteDebugProtocolClient extends IDebugProtocolClient, JavaSourceResolver {

    /**
     * Event/notification sent by the server to event-based clients (VS Code, etc.)
     * requesting the resolution of a Java source corresponding to a Qute template.
     *
     * <p>
     * The payload contains a unique request ID and the original
     * {@link JavaSourceLocationArguments}. The client is expected to
     * eventually send back a {@code qute/onJavaSourceResolved} notification
     * with the same ID and the resolved source.
     * </p>
     *
     * <p>
     * Default implementation is a no-op. Clients that support event-based
     * resolution should override this method.
     * </p>
     *
     * @param request the event-based request containing ID and source arguments
     */
    @JsonNotification("qute/onResolveJavaSource")
    default void onResolveJavaSource(JavaSourceLocationEventArguments request) {
        // no-op by default
        // Event-based clients (VS Code) should override and later send back:
        // qute/onJavaSourceResolved with the same request ID
    }
}
