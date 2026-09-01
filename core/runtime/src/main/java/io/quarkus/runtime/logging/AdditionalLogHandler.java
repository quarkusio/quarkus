package io.quarkus.runtime.logging;

import java.util.logging.Handler;

/**
 * A wrapper carrying an extension-contributed root log handler as a service value.
 * <p>
 * RECORDER COMPAT: this type exists only to bridge legacy {@code LogHandlerBuildItem} contributions (which
 * carry a {@code RuntimeValue<Optional<Handler>>}) into the logging service graph. The logging configuration
 * service consumes all services of this type, installs the shared error manager and cleanup filter on each,
 * and attaches them to the root logger. It can be removed once all handler-contributing extensions produce
 * {@link Handler} services directly.
 *
 * @param handler the contributed handler (must not be {@code null})
 */
public record AdditionalLogHandler(Handler handler) {
}
