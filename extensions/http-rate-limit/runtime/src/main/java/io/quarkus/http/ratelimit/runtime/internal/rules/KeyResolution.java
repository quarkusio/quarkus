package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitRejectionInfo.Reason;
import io.quarkus.http.ratelimit.RateLimitResolver;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLogger;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLoggers;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
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
    private final RateLimitRuleConfig rule;
    private final DefaultRateLimitPolicy staticPolicy;
    private final DefaultRateLimitPolicy fallbackPolicy;
    private final HttpRateLimitRuntimeConfig config;
    private final RateLimitKeyResolver resolver;
    // an immediate resolver, as the built-in ones are, answers at once: only another resolver costs a timer
    private final boolean boundedResolver;
    private final RateLimitResolver limitResolver;
    private final ThrottledLogger resolverWarning;
    private final ThrottledLogger limitWarning;

    KeyResolution(RateLimitRuleConfig rule, RateLimitKeyResolver resolver, RateLimitResolver limitResolver,
            HttpRateLimitRuntimeConfig config, ThrottledLoggers loggers) {
        this.rule = rule;
        this.staticPolicy = rule.limit().map(limit -> DefaultRateLimitPolicy.of(rule, limit)).orElse(null);
        this.fallbackPolicy = rule.fallback().map(limit -> DefaultRateLimitPolicy.of(rule, limit)).orElse(null);
        this.config = config;
        this.resolver = resolver;
        this.boundedResolver = !(resolver instanceof ImmediateKeyResolver);
        this.limitResolver = limitResolver;
        this.resolverWarning = loggers.logger(LOG);
        this.limitWarning = loggers.logger(LOG);
    }

    /**
     * Resolves the request key, then selects its limit. Storage normalization does not change what the limit resolver
     * sees in the request context. An empty key intentionally skips the rule; failures follow the configured policy.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Uni<Result> resolve(RoutingContext ctx, Optional<SecurityIdentity> identity, Executor onContext) {
        RateLimitContextImpl context = new RateLimitContextImpl(ctx, identity, rule.name(), rule.phase());
        Uni<Optional<String>> key = Uni.createFrom().deferred(new Supplier<>() {
            @Override
            public Uni<? extends Optional<String>> get() {
                return resolver.resolve(context);
            }
        });
        if (boundedResolver) {
            key = bounded(key, config.keyResolverTimeout());
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
                        if (config.keyResolverFailure() == HttpRateLimitRuntimeConfig.KeyResolverFailure.REJECT) {
                            throw new IllegalStateException("Rate limit key resolution failed", failure);
                        }
                        warn(resolverWarning, failure, config.keyResolverTimeout(),
                                "Rate limit rule '%s' key resolution took longer than %dms; the request is keyed by its"
                                        + " client address",
                                "Rate limit rule '%s' key resolution failed; the request is keyed by its client address");
                        return Optional.of(IpKeyResolver.key(ctx.request()));
                    }
                })
                // the resolver may complete on any thread: the limit resolver runs on the request's context
                .emitOn(onContext)
                .onItem().transformToUni(new Function<Optional<String>, Uni<? extends Result>>() {
                    @Override
                    public Uni<? extends Result> apply(Optional<String> key) {
                        return key.isEmpty()
                                ? Uni.createFrom().item(SKIP)
                                : resolvePolicy(context, RateLimitKeys.cap(key.get()));
                    }
                }).onFailure().recoverWithItem(new Function<>() {
                    @Override
                    public Result apply(Throwable failure) {
                        resolverWarning.warnf(failure, "Rate limit rule '%s' key resolution failed; answering 503",
                                rule.name());
                        return new Unavailable(null, Reason.KEY_RESOLUTION_FAILED);
                    }
                });
    }

    private Uni<Result> resolvePolicy(RateLimitContextImpl context, String key) {
        if (limitResolver == null) {
            return Uni.createFrom().item(new Resolved(key, staticPolicy));
        }
        return bounded(Uni.createFrom().deferred(new Supplier<Uni<? extends RateLimit>>() {
            @Override
            public Uni<? extends RateLimit> get() {
                return limitResolver.resolve(context);
            }
        }), config.limitResolverTimeout())
                .map(new Function<RateLimit, Result>() {
                    @Override
                    public Result apply(RateLimit limit) {
                        if (limit == null) {
                            throw new IllegalArgumentException("The limit resolver emitted null");
                        }
                        return new Resolved(key, DefaultRateLimitPolicy.of(rule, limit));
                    }
                })
                .onFailure().recoverWithItem(new Function<>() {
                    @Override
                    public Result apply(Throwable failure) {
                        limitWarning.warnf(failure, "Rate limit rule '%s' limit resolution failed; "
                                + (rule.fallback().isPresent() ? "using fallback-limit" : "answering 503"), rule.name());
                        return fallbackPolicy == null ? new Unavailable(key, Reason.LIMIT_RESOLUTION_FAILED)
                                : new Resolved(key, fallbackPolicy);
                    }
                });
    }

    /**
     * Logs a failure, at most once per warning interval for each warning.
     *
     * @param timeout the message of a timeout, with the rule and the timeout in milliseconds
     * @param failed the message of any other failure, with the rule, logged with the failure
     */
    private void warn(ThrottledLogger warning, Throwable failure, Duration duration, String timeout, String failed) {
        if (failure instanceof TimeoutException) {
            warning.warnf(timeout, rule.name(), duration.toMillis());
        } else {
            warning.warnf(failure, failed, rule.name());
        }
    }

    private static <T> Uni<T> bounded(Uni<T> uni, Duration timeout) {
        return uni.ifNoItem().after(timeout).failWith(new Supplier<>() {
            @Override
            public Throwable get() {
                return new TimeoutException();
            }
        });
    }

    record Unavailable(String key, Reason reason) implements Result {
    }

    sealed interface Result {
    }

    /**
     * The rule does not apply to this request.
     */
    record Skip() implements Result {
    }

    /**
     * @param policy the rule's policy, or the one its limit resolver returned for this key
     */
    record Resolved(String key, DefaultRateLimitPolicy policy) implements Result {
    }
}
