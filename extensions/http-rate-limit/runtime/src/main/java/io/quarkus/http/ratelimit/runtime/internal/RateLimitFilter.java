package io.quarkus.http.ratelimit.runtime.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.arc.Arc;
import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitRejectionInfo;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.keys.IpKeyResolver;
import io.quarkus.http.ratelimit.runtime.internal.rules.Outcome;
import io.quarkus.http.ratelimit.runtime.internal.rules.RateLimitRules;
import io.quarkus.http.ratelimit.runtime.internal.rules.RateLimitRules.RuleMembership;
import io.quarkus.http.ratelimit.runtime.internal.rules.RuleEvaluator;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;

/**
 * The HTTP filters that apply the rate limit rules: one per phase, before and after authentication. For each request,
 * a filter evaluates the rules whose paths and methods match, then passes the request on or refuses it. It fires a
 * {@link RateLimitRejectionInfo} event for every rejection.
 * <p>
 * The rules that match a request are evaluated one after another, narrowest window first, and the first refusal
 * stops the chain, so a wider rule never spends a permit on a request a narrower rule refused. The reverse case (a
 * narrow rule admits, a wider one rejects) leaves one permit of the narrow rule spent; it recovers within the narrow
 * window.
 */
@Singleton
public class RateLimitFilter {

    private static final Logger LOG = Logger.getLogger(RateLimitFilter.class);

    /**
     * Route order -250: after host validation (-400) and CORS (-300), before authentication (-200).
     */
    private static final int PRE_AUTHENTICATION_PRIORITY = 250;

    /**
     * Route order -120: after authentication (-200) and form authentication (-150), before authorization (-100).
     */
    private static final int POST_AUTHENTICATION_PRIORITY = 120;

    /**
     * A copy of {@code HttpLoadShedding.SKIP_SAMPLE_KEY}, so that load shedding does not take the latency of a rejected
     * request as a sample, without depending on it.
     */
    public static final String SKIP_SAMPLE_KEY = "io.quarkus.load.shedding.skip-sample";

    private final boolean enabled;
    /**
     * {@code null} when rate limiting is disabled.
     */
    private final RateLimitRules rules;
    private final DefaultRateLimitRejectionWriter writer;
    private final Event<RateLimitRejectionInfo> events;
    private final boolean syncObservers;
    private final boolean asyncObservers;
    // quarkus.http.limits.max-body-size, -1 when unbounded
    private final long maxBodySize;
    private final ThrottledLogger warning;

    @Inject
    RateLimitFilter(HttpRateLimitRuntimeConfig config,
            Instance<RateLimitRules> rules,
            DefaultRateLimitRejectionWriter writer,
            Event<RateLimitRejectionInfo> events,
            ThrottledLoggers loggers,
            VertxHttpConfig httpConfig) {
        this.enabled = config.enabled();
        this.maxBodySize = httpConfig.limits().maxBodySize().isPresent()
                ? httpConfig.limits().maxBodySize().get().asLongValue()
                : -1;
        // when rate limiting is disabled, nothing is built: the rules validate the runtime configuration
        this.rules = enabled ? rules.get() : null;
        this.writer = writer;
        this.events = events;
        List<? extends ObserverMethod<? super RateLimitRejectionInfo>> observers = Arc.container()
                .resolveObserverMethods(RateLimitRejectionInfo.class);
        boolean anySync = false;
        boolean anyAsync = false;
        for (ObserverMethod<? super RateLimitRejectionInfo> observer : observers) {
            if (observer.isAsync()) {
                anyAsync = true;
            } else {
                anySync = true;
            }
        }
        this.syncObservers = anySync;
        this.asyncObservers = anyAsync;
        this.warning = loggers.logger(LOG);
    }

    void init(@Observes Filters filters) {
        if (!enabled) {
            return;
        }
        for (RateLimitPhase phase : RateLimitPhase.values()) {
            if (!rules.evaluators(phase).isEmpty()) {
                int priority = switch (phase) {
                    case PRE_AUTHENTICATION -> PRE_AUTHENTICATION_PRIORITY;
                    case POST_AUTHENTICATION -> POST_AUTHENTICATION_PRIORITY;
                };
                filters.register(new PhaseFilter(phase), priority);
            }
        }
    }

    private final class PhaseFilter implements Handler<RoutingContext> {

        private final RateLimitPhase phase;

        PhaseFilter(RateLimitPhase phase) {
            this.phase = phase;
        }

        @Override
        public void handle(RoutingContext ctx) {
            RateLimitFilter.this.handle(ctx, phase);
        }
    }

    private void handle(RoutingContext ctx, RateLimitPhase phase) {
        // the canonical path that HTTP permissions match too: the REST layer ignores matrix parameters, so /login;x=1
        // must be limited like /login
        String routedPath = ctx.normalizedPath();
        String path = HttpSecurityUtils.normalizePath(routedPath);
        // the router keeps %2F and \ inside a segment, so /api/items/..%2F..%2Fpublic reaches /api/items/{id} although
        // its canonical path is /public: like HTTP permissions, apply the rules of both paths
        String method = ctx.request().method().name();
        List<RuleEvaluator> evaluators = new ArrayList<>();
        for (RuleMembership membership : rules.matcher(phase).match(path, routedPath)) {
            if (membership.appliesTo(method)) {
                evaluators.add(membership.evaluator());
            }
        }
        if (evaluators.isEmpty()) {
            ctx.next();
            return;
        }

        Context context = ctx.vertx().getOrCreateContext();
        // resolvers and stores may complete on any thread: every evaluator, and the response, run on the request's context
        Executor onContext = new Executor() {
            @Override
            public void execute(Runnable task) {
                if (Vertx.currentContext() == context) {
                    task.run();
                } else {
                    context.runOnContext(new Handler<>() {
                        @Override
                        public void handle(Void ignored) {
                            task.run();
                        }
                    });
                }
            }
        };
        AtomicReference<Cancellable> subscription = new AtomicReference<>();
        // invoked with a failed result on connection close, response exception and HTTP/2 stream reset; never use the
        // connection's close handler, which has a single slot that Quarkus uses for max-connections accounting
        ctx.addEndHandler(new Handler<>() {
            @Override
            public void handle(AsyncResult<Void> ar) {
                Cancellable cancellable = subscription.get();
                if (ar.failed() && cancellable != null) {
                    cancellable.cancel();
                }
            }
        });
        if (phase == RateLimitPhase.PRE_AUTHENTICATION) {
            subscription.set(evaluate(ctx, Optional.empty(), onContext, evaluators, 0, new ArrayList<>())
                    .subscribe().with(new Consumer<>() {
                        @Override
                        public void accept(List<Outcome> outcomes) {
                            respond(ctx, Optional.empty(), outcomes);
                        }
                    }, new Consumer<>() {
                        @Override
                        public void accept(Throwable failure) {
                            // load shedding must not take the latency of a failed request as a sample
                            ctx.put(SKIP_SAMPLE_KEY, true);
                            ctx.fail(failure);
                        }
                    }));
            return;
        }

        // under quarkus.http.auth.proactive=false, this forces authentication; a failure is answered by the
        // authentication failure handler as soon as it happens, possibly before it reaches this handler, so the request
        // is marked beforehand: load shedding must never take the latency of that response as a sample
        boolean skipSample = Boolean.TRUE.equals(ctx.get(SKIP_SAMPLE_KEY));
        ctx.put(SKIP_SAMPLE_KEY, true);
        AtomicReference<Optional<SecurityIdentity>> identity = new AtomicReference<>(Optional.empty());
        subscription.set(QuarkusHttpUser.getSecurityIdentity(ctx, null)
                .emitOn(onContext)
                .onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends List<Outcome>>>() {
                    @Override
                    public Uni<? extends List<Outcome>> apply(SecurityIdentity resolved) {
                        if (!skipSample) {
                            ctx.remove(SKIP_SAMPLE_KEY);
                        }
                        identity.set(Optional.ofNullable(resolved));
                        return evaluate(ctx, identity.get(), onContext, evaluators, 0, new ArrayList<>());
                    }
                })
                .subscribe().with(new Consumer<>() {
                    @Override
                    public void accept(List<Outcome> outcomes) {
                        respond(ctx, identity.get(), outcomes);
                    }
                }, new Consumer<>() {
                    @Override
                    public void accept(Throwable failure) {
                        failed(ctx, failure);
                    }
                }));
    }

    /**
     * Follows {@code AbstractHttpAuthorizer}: under lazy authentication, the deferred identity has already handed an
     * authentication failure to the authentication failure handler, which either ended the response with a challenge
     * or failed the request itself. Failing it again would process the failure twice.
     */
    private static void failed(RoutingContext ctx, Throwable failure) {
        ctx.put(SKIP_SAMPLE_KEY, true);
        if (!ctx.response().ended() && !failure.equals(ctx.failure())) {
            ctx.fail(failure);
        } else if (failure instanceof AuthenticationFailedException) {
            LOG.debug("Authentication challenge is required");
        } else if (failure instanceof AuthenticationRedirectException redirect) {
            LOG.debugf("Completing authentication with a redirect to %s", redirect.getRedirectUri());
        } else {
            LOG.debugf(failure, "The request was already completed when it failed");
        }
    }

    private Uni<List<Outcome>> evaluate(RoutingContext ctx, Optional<SecurityIdentity> identity,
            Executor onContext, List<RuleEvaluator> evaluators, int index,
            List<Outcome> outcomes) {
        if (index == evaluators.size()) {
            return Uni.createFrom().item(outcomes);
        }
        RuleEvaluator evaluator = evaluators.get(index);
        return evaluator.evaluate(ctx, identity).emitOn(onContext).onItem()
                .transformToUni(new Function<>() {
                    @Override
                    public Uni<? extends List<Outcome>> apply(Outcome outcome) {
                        outcomes.add(outcome);
                        if (blocks(outcome)) {
                            return Uni.createFrom().item(outcomes);
                        }
                        return evaluate(ctx, identity, onContext, evaluators, index + 1, outcomes);
                    }
                });
    }

    /**
     * @return whether the outcome refuses the request
     */
    private static boolean blocks(Outcome outcome) {
        return switch (outcome.kind()) {
            case DECIDED -> !(outcome.decision() instanceof RateLimitDecision.Allowed);
            case SKIPPED -> false;
        };
    }

    /**
     * @param identity the identity known after authentication; empty before
     */
    private void respond(RoutingContext ctx, Optional<SecurityIdentity> identity, List<Outcome> outcomes) {
        // the chain stops at the first refusal, so only the last outcome can refuse the request
        Outcome last = outcomes.getLast();
        if (!blocks(last)) {
            ctx.next();
            return;
        }

        ctx.put(SKIP_SAMPLE_KEY, true);
        // the body of a refused request is never read: drain it, or the connection cannot serve the next request
        if (!ctx.request().isEnded()) {
            drain(ctx);
        }
        if (last.decision() instanceof RateLimitDecision.Rejected rejected) {
            reject(ctx, identity, last, rejected);
        }
    }

    /**
     * Drains the body of a refused request, but no further than {@code quarkus.http.limits.max-body-size}, the most a
     * served request may send, so that a refused client cannot hold a connection with a huge or endless body: a body
     * declared larger is never read and is cut off after the response, and a body that grows larger is cut off as soon
     * as it does.
     */
    private void drain(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        if (maxBodySize < 0) {
            request.resume();
            return;
        }
        // the HTTP decoder refuses a malformed Content-Length with 400 before any route runs
        String length = request.getHeader(HttpHeaderNames.CONTENT_LENGTH);
        if (length != null && Long.parseLong(length) > maxBodySize) {
            cutOff(ctx);
            return;
        }
        request.handler(new Handler<>() {
            private long received;
            private boolean closed;

            @Override
            public void handle(Buffer chunk) {
                received += chunk.length();
                if (received > maxBodySize && !closed) {
                    closed = true;
                    cutOff(ctx);
                }
            }
        });
        request.resume();
    }

    /**
     * Stops the client from sending the rest of the body once the response has ended, like
     * {@code quarkus.http.limits.max-body-size} does.
     */
    private static void cutOff(RoutingContext ctx) {
        if (ctx.response().ended()) {
            stopReading(ctx);
            return;
        }
        HttpVersion version = ctx.request().version();
        if (version != HttpVersion.HTTP_2 && version != HttpVersion.HTTP_3) {
            ctx.response().putHeader(HttpHeaderNames.CONNECTION, "close");
        }
        ctx.addEndHandler(new Handler<>() {
            @Override
            public void handle(AsyncResult<Void> ignored) {
                stopReading(ctx);
            }
        });
    }

    /**
     * Over HTTP/1.x, closes the connection. Over HTTP/2 and HTTP/3 the connection carries other requests, so only the
     * stream of this request ends: over HTTP/2 with a reset of code {@code NO_ERROR}, which asks the client to stop
     * sending and not to discard the response (RFC 9113, section 8.1), and over HTTP/3 by aborting the reading of the
     * stream, after which the client must not discard the response either (RFC 9114, section 4.1).
     */
    private static void stopReading(RoutingContext ctx) {
        switch (ctx.request().version()) {
            case HTTP_2 -> ctx.vertx().runOnContext(new Handler<>() {
                @Override
                public void handle(Void ignored) {
                    // in a later task: the response is only sent when the connection flushes, at the end of the
                    // current read, and a reset drops what the stream has not sent yet
                    ctx.response().reset(0);
                }
            });
            case HTTP_3 -> ctx.response().cancel();
            default -> ctx.request().connection().close();
        }
    }

    private void reject(RoutingContext ctx, Optional<SecurityIdentity> identity, Outcome outcome,
            RateLimitDecision.Rejected rejected) {
        RuleEvaluator evaluator = outcome.evaluator();
        long retryAfterSeconds = RetryAfter.seconds(rejected.retryAfter());
        ctx.response().putHeader(HttpHeaderNames.RETRY_AFTER, Long.toString(retryAfterSeconds));
        fire(rejection(ctx, identity, evaluator, outcome.key(), retryAfterSeconds));
        writer.write(ctx);
    }

    /**
     * @return a snapshot that holds no reference to the request, as async observers may run after it has ended
     */
    private static RateLimitRejectionInfo rejection(RoutingContext ctx, Optional<SecurityIdentity> identity,
            RuleEvaluator evaluator, String key, long retryAfterSeconds) {
        Optional<String> principal = Optional.empty();
        if (identity.isPresent() && !identity.get().isAnonymous()) {
            principal = Optional.ofNullable(identity.get().getPrincipal().getName());
        }
        return new DefaultRateLimitRejectionInfo(evaluator.rule(), evaluator.phase(), key, retryAfterSeconds,
                ctx.request().method().name(), HttpSecurityUtils.normalizePath(ctx.normalizedPath()),
                IpKeyResolver.clientAddress(ctx.request()),
                principal);
    }

    // a failing observer must neither hang the request nor change its outcome
    private void fire(RateLimitRejectionInfo rejection) {
        if (syncObservers) {
            try {
                events.fire(rejection);
            } catch (RuntimeException e) {
                warning.warn("An observer of RateLimitRejectionInfo failed; the request continues", e);
            }
        }
        if (asyncObservers) {
            fireAsync(rejection);
        }
    }

    /**
     * A failing async observer completes the stage exceptionally; ArC has already logged it, through
     * {@code AsyncObserverExceptionHandler}.
     */
    private void fireAsync(RateLimitRejectionInfo rejection) {
        try {
            events.fireAsync(rejection);
        } catch (RuntimeException e) {
            warning.warn("Firing RateLimitRejectionInfo to @ObservesAsync observers failed; the request continues", e);
        }
    }
}
