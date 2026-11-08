package io.quarkus.vertx.http.runtime.security;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A non-generic Supplier of additional security event properties to support
 * ServiceRegistrar service registration without generic types.
 */
public interface AdditionalSecurityEventProps extends Supplier<Map<String, Object>> {
}
