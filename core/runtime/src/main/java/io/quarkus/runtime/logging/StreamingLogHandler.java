package io.quarkus.runtime.logging;

import java.util.logging.Handler;

/**
 * A wrapper carrying the Dev UI streaming log handler as a service value.
 * <p>
 * RECORDER COMPAT: this type exists only to bridge the legacy {@code StreamingLogHandlerBuildItem} (which
 * carries a {@code RuntimeValue<Optional<Handler>>}) into the logging service graph. The logging
 * configuration service optionally consumes a service of this type and installs it with a cleanup filter and
 * the startup banner, matching the previous behavior. It can be removed once the Dev UI extension produces a
 * {@link Handler} service directly.
 *
 * @param handler the streaming handler (must not be {@code null})
 */
public record StreamingLogHandler(Handler handler) {
}
