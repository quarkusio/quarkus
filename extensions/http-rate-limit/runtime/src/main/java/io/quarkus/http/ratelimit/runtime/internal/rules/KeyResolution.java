package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitOverride;
import io.quarkus.http.ratelimit.RateLimitPolicy;
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
     * The bound on key resolution, and separately on the override, for one request.
     */
    static final Duration TIMEOUT = Duration.ofMillis(250);

    private final DefaultRateLimitPolicy policy;
    private final RateLimitKeyResolver resolver;
    // an immediate resolver, as the built-in ones are, answers at once: only another resolver costs a timer
    private final boolean boundedResolver;
    private final RateLimitOverride override;
    private final ThrottledLogger resolverWarning;
    private final ThrottledLogger overrideWarning;

    KeyResolution(DefaultRateLimitPolicy policy, RateLimitKeyResolver resolver, RateLimitOverride override,
            ThrottledLoggers loggers) {
        this.policy = policy;
        this.resolver = resolver;
        this.boundedResolver = !(resolver instanceof ImmediateKeyResolver);
        this.override = override;
        this.resolverWarning = loggers.logger(LOG);
        this.overrideWarning = loggers.logger(LOG);
    }

    /**
     * Resolves the key and the override. {@link Skip} only when the resolver returns no key. A failure never lets the
     * request escape the rule, so that a client that can make a resolver or an override fail cannot switch the rule
     * off:
     * <ul>
     * <li>a resolver that fails, or does not answer within {@link #TIMEOUT}, keys the request by its client address,
     * like a request without the header of a {@code header:} key;</li>
     * <li>an override that fails, does not answer within {@link #TIMEOUT}, or returns an invalid limit, leaves the key
     * with the configured limit.</li>
     * </ul>
     * Each kind of failure is logged at most once per warning interval.
     * <p>
     * A key longer than {@value RateLimitKeys#MAX_KEY_LENGTH} characters is capped by {@link RateLimitKeys#cap(String)}
     * before the override sees it.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Uni<Result> resolve(RoutingContext ctx, Optional<SecurityIdentity> identity, Executor onContext) {
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
                // the resolver may complete on any thread: the override runs on the request's context
                .emitOn(onContext)
                .onItem().transformToUni(new Function<>() {
                    @Override
                    public Uni<? extends Result> apply(Optional<String> key) {
                        return key.isEmpty()
                                ? Uni.createFrom().item(SKIP)
                                : resolvePolicy(context, RateLimitKeys.cap(key.get()));
                    }
                });
    }

    private Uni<Result> resolvePolicy(RateLimitContextImpl context, String key) {
        if (override == null) {
            return Uni.createFrom().item(new Resolved(key, policy));
        }
        return bounded(Uni.createFrom().deferred(new Supplier<Uni<? extends Optional<RateLimitOverride.Limit>>>() {
            @Override
            public Uni<? extends Optional<RateLimitOverride.Limit>> get() {
                return override.override(context, key);
            }
        }))
                .map(new Function<Optional<RateLimitOverride.Limit>, Result>() {
                    @Override
                    public Result apply(Optional<RateLimitOverride.Limit> limit) {
                        if (limit.isEmpty()) {
                            return new Resolved(key, policy);
                        }
                        RateLimitOverride.Limit l = limit.get();
                        // throws IllegalArgumentException on an invalid limit, handled like a failing override
                        return new Resolved(key, new DefaultRateLimitPolicy(policy.rule(), policy.phase(), l.permits(),
                                l.window(), l.burst().orElse(l.permits()), policy.maxKeys()));
                    }
                })
                .onFailure().recoverWithItem(new Function<>() {
                    @Override
                    public Result apply(Throwable failure) {
                        warn(overrideWarning, failure,
                                "Rate limit rule '%s' override took longer than %dms; the key has the configured limit",
                                "Rate limit rule '%s' override failed; the key has the configured limit");
                        return new Resolved(key, policy);
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

    /**
     * @param policy the rule's policy, or the one its override returned for this key
     */
    record Resolved(String key, RateLimitPolicy policy) implements Result {
    }
}
