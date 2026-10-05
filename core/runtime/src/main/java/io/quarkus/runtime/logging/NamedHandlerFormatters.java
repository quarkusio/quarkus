package io.quarkus.runtime.logging;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Formatter;

/**
 * A wrapper carrying the per-named-handler formatters as a service value.
 * <p>
 * RECORDER COMPAT: this type exists only to bridge legacy {@code LogNamedHandlerFormatBuildItem} contributions
 * (which carry a {@code RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>>}) into the
 * logging service graph. The logging configuration service consumes a service of this type when building the
 * named handlers. It can be removed once formatter-contributing extensions produce {@link Formatter} services
 * directly.
 *
 * @param formatters the formatters keyed by handler type and handler name (must not be {@code null})
 */
public record NamedHandlerFormatters(Map<NamedHandlerType, Map<String, Optional<Formatter>>> formatters) {
}
