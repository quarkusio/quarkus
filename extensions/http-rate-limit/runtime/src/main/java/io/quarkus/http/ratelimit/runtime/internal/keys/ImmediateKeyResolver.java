package io.quarkus.http.ratelimit.runtime.internal.keys;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;

/**
 * A key resolver that answers at once and never fails, as the built-in ones do: its key resolution needs no timer. Not
 * part of the public SPI, since a resolver that claimed it but did I/O would lose the bound on slow resolvers.
 */
public interface ImmediateKeyResolver extends RateLimitKeyResolver {
}
