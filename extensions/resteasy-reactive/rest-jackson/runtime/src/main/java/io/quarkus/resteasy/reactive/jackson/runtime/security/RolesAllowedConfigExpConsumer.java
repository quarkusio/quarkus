package io.quarkus.resteasy.reactive.jackson.runtime.security;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Non-generic view of the {@link RolesAllowedConfigExpStorage} so that it can be used as a service type.
 */
public interface RolesAllowedConfigExpConsumer extends BiConsumer<String, Supplier<String[]>> {
}
