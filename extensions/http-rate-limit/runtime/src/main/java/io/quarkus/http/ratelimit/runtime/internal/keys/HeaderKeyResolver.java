package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.util.List;
import java.util.Optional;

import io.quarkus.http.ratelimit.RateLimitContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;

/**
 * Not a bean: built from a {@code request-key} of the form {@code header:<name>}.
 * <p>
 * A request without the header, or with a blank value, is keyed by its client address, like the {@code ip} key: like
 * the {@code principal} key for an anonymous request, so that leaving the header out does not escape the limit.
 * <p>
 * The value is often a credential, such as an API key, so the key holds its SHA-256 instead: the value never reaches a
 * store, a rejection event or a log.
 * <p>
 * Every request that sends the header more than once is keyed by {@value #REPEATED}, a single bucket: the code that
 * reads the header may use another occurrence than the first, so a client could otherwise authenticate with one value
 * while each request got a fresh bucket from another.
 */
public final class HeaderKeyResolver implements ImmediateKeyResolver {

    static final String REPEATED = "header:repeated";

    private final String headerName;

    public HeaderKeyResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public Uni<Optional<String>> resolve(RateLimitContext context) {
        HttpServerRequest request = context.routingContext().request();
        // the Vert.x header lookup is case-insensitive
        List<String> values = request.headers().getAll(headerName);
        if (values.size() > 1) {
            // the code that reads the header, an authentication mechanism or an override, may pick another occurrence
            // than the first: every request that repeats it shares one bucket, so repeating it never earns a fresh one
            return Uni.createFrom().item(Optional.of(REPEATED));
        }
        if (values.isEmpty() || values.getFirst().isBlank()) {
            return Uni.createFrom().item(Optional.of(IpKeyResolver.key(request)));
        }
        return Uni.createFrom().item(Optional.of("header:" + RateLimitKeys.sha256(values.getFirst().trim())));
    }
}
