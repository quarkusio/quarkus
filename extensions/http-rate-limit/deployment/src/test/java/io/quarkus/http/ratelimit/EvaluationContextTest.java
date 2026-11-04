package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * The first rule's resolver completes on a thread of its own, without a Vert.x context; the second rule's resolver
 * must still run on the request context.
 * <p>
 * A key resolution timeout is not used to get there: Quarkus fires Mutiny timeouts on the Vert.x context they were
 * set from.
 */
public class EvaluationContextTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";
    private static final String CONTEXT_KEY = "test.request-context";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, ForeignThreadResolver.class, ContextCheckingResolver.class,
                    ContextRecorder.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PREFIX + "rules.foreign.paths", "/*")
            .overrideConfigKey(PREFIX + "rules.foreign.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "rules.foreign.request-key", "custom:foreign-thread")
            .overrideRuntimeConfigKey(PREFIX + "rules.foreign.permits", "10")
            .overrideRuntimeConfigKey(PREFIX + "rules.foreign.window", "1m")
            .overrideConfigKey(PREFIX + "rules.checking.paths", "/*")
            .overrideConfigKey(PREFIX + "rules.checking.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "rules.checking.request-key", "custom:context-check")
            .overrideRuntimeConfigKey(PREFIX + "rules.checking.permits", "10")
            .overrideRuntimeConfigKey(PREFIX + "rules.checking.window", "1h");

    static final AtomicReference<Boolean> onRequestContext = new AtomicReference<>();

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void nextResolverRunsOnTheRequestContext() {
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        assertThat(onRequestContext.get()).isTrue();
    }

    @Singleton
    @Identifier("foreign-thread")
    public static class ForeignThreadResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(Optional.of("k"))).start());
        }
    }

    @Singleton
    @Identifier("context-check")
    public static class ContextCheckingResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            Context requestContext = context.routingContext().get(CONTEXT_KEY);
            onRequestContext.set(requestContext != null && Vertx.currentContext() == requestContext);
            return Uni.createFrom().item(Optional.of("k"));
        }
    }

    @Singleton
    public static class ContextRecorder {

        void init(@Observes Router router) {
            router.route().order(Integer.MIN_VALUE + 2).handler(ctx -> {
                ctx.put(CONTEXT_KEY, Vertx.currentContext());
                ctx.next();
            });
        }
    }
}
