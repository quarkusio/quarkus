package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.util.Optional;
import java.util.function.Function;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.keys.RateLimitKeys;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Resolves the key and the policy of a request for one rule.
 */
final class KeyResolution {

    private static final Result SKIP = new Skip();

    private final DefaultRateLimitPolicy policy;
    private final RateLimitKeyResolver resolver;

    KeyResolution(DefaultRateLimitPolicy policy, RateLimitKeyResolver resolver) {
        this.policy = policy;
        this.resolver = resolver;
    }

    /**
     * Resolves the key. {@link Skip} when the resolver returns no key.
     * <p>
     * A key longer than {@value RateLimitKeys#MAX_KEY_LENGTH} characters is capped by {@link RateLimitKeys#cap(String)}.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Uni<Result> resolve(RoutingContext ctx, Optional<SecurityIdentity> identity) {
        RateLimitContextImpl context = new RateLimitContextImpl(ctx, identity, policy.rule(), policy.phase());
        return resolver.resolve(context).map(new Function<Optional<String>, Result>() {
            @Override
            public Result apply(Optional<String> key) {
                return key.isEmpty() ? SKIP : new Resolved(RateLimitKeys.cap(key.get()));
            }
        });
    }

    sealed interface Result {
    }

    /**
     * The rule does not apply to this request.
     */
    record Skip() implements Result {
    }

    record Resolved(String key) implements Result {
    }
}
