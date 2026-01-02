package io.quarkus.qute.debug.client;

import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

/**
 * Combined Debug Protocol client interface for Qute templates.
 * <p>
 * Extends the standard {@link IDebugProtocolClient} to include
 * the {@link JavaSourceResolver} functionality for resolving
 * Java sources referenced from Qute templates via {@code qute-java://} URIs.
 * </p>
 *
 * <p>
 * Implementations of this interface can:
 * <ul>
 * <li>Receive standard DAP events and messages.</li>
 * <li>Resolve Java source locations corresponding to Qute templates,
 * enabling breakpoints in template-related Java code.</li>
 * </ul>
 * </p>
 */
public interface QuteDebugProtocolClient extends IDebugProtocolClient, JavaSourceResolver {

}