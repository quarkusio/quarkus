package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLogger;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLoggers;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.keys.ImmediateKeyResolver;
import io.quarkus.http.ratelimit.runtime.internal.keys.IpKeyResolver;
import io.quarkus.http.ratelimit.runtime.internal.keys.RateLimitKeys;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Resolves the key and the policy of a request for one rule.
 */
final class KeyResolution {

    private static final Logger LOG = Logger.getLogger(KeyResolution.class);

    private static final Result SKIP = new Skip();
    /**
     * The bound on key resolution for one request.
     */
    static final Duration TIMEOUT = Duration.ofMillis(250);

    private final DefaultRateLimitPolicy policy;
    private final RateLimitKeyResolver resolver;
    // an immediate resolver, as the built-in ones are, answers at once: only another resolver costs a timer
    private final boolean boundedResolver;
    private final ThrottledLogger resolverWarning;

    KeyResolution(DefaultRateLimitPolicy policy, RateLimitKeyResolver resolver, ThrottledLoggers loggers) {
        this.policy = policy;
        this.resolver = resolver;
        this.boundedResolver = !(resolver instanceof ImmediateKeyResolver);
        this.resolverWarning = loggers.logger(LOG);
    }

    /**
     * Resolves the key. {@link Skip} only when the resolver returns no key. A user supplied resolver that fails, or does
     * not answer within {@link #TIMEOUT}, keys the request by its client address, like a request without the header of
     * a {@code header:} key, so that a client that can make a resolver fail cannot switch the rule off; a failure is
     * logged at most once per warning interval.
     * <p>
     * A key longer than {@value RateLimitKeys#MAX_KEY_LENGTH} characters is capped by {@link RateLimitKeys#cap(String)}.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Uni<Result> resolve(RoutingContext ctx, Optional<SecurityIdentity> identity) {
        RateLimitContextImpl context = new RateLimitContextImpl(ctx, identity, policy.rule(), policy.phase());
        Uni<Optional<String>> key = Uni.createFrom().deferred(new Supplier<>() {
            @Override
            public Uni<? extends Optional<String>> get() {
                return resolver.resolve(context);
            }
        });
        if (boundedResolver) {
            key = bounded(key);
        }
        return key
                // a resolver that emits null instead of an Optional is handled like one that fails
                .onItem().ifNull().failWith(new Supplier<>() {
                    @Override
                    public Throwable get() {
                        return new NullPointerException("The key resolver emitted null instead of an Optional");
                    }
                })
                .onFailure().recoverWithItem(new Function<>() {
                    @Override
                    public Optional<String> apply(Throwable failure) {
                        warn(resolverWarning, failure,
                                "Rate limit rule '%s' key resolution took longer than %dms; the request is keyed by its"
                                        + " client address",
                                "Rate limit rule '%s' key resolution failed; the request is keyed by its client address");
                        return Optional.of(IpKeyResolver.key(ctx.request()));
                    }
                })
                .onItem().transformToUni(new Function<>() {
                    @Override
                    public Uni<? extends Result> apply(Optional<String> key) {
                        return key.isEmpty()
                                ? Uni.createFrom().item(SKIP)
                                : Uni.createFrom().item(new Resolved(RateLimitKeys.cap(key.get())));
                    }
                });
    }

    /**
     * Logs a failure, at most once per warning interval for each warning.
     *
     * @param timeout the message of a timeout, with the rule and the timeout in milliseconds
     * @param failed the message of any other failure, with the rule, logged with the failure
     */
    private void warn(ThrottledLogger warning, Throwable failure, String timeout, String failed) {
        if (failure instanceof TimeoutException) {
            warning.warnf(timeout, policy.rule(), TIMEOUT.toMillis());
        } else {
            warning.warnf(failure, failed, policy.rule());
        }
    }

    private static <T> Uni<T> bounded(Uni<T> uni) {
        return uni.ifNoItem().after(TIMEOUT).failWith(new Supplier<>() {
            @Override
            public Throwable get() {
                return new TimeoutException();
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
