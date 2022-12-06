package io.quarkus.vertx.web.context;

import static io.restassured.RestAssured.get;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * Verify that reactive routes are called on duplicated contexts and that they handled them properly.
 */
public class DuplicatedContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyRoutes.class));

    @Test
    public void testThatRoutesAreCalledOnDuplicatedContext() {
        // Creates a bunch of requests that will be executed concurrently.
        // So, we are sure that event loops are reused.
        List<Uni<Void>> unis = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String uuid = UUID.randomUUID().toString();
            unis.add(Uni.createFrom().item(() -> {
                String resp = get("/context/" + uuid).asString();
                Assertions.assertEquals(resp, "OK-" + uuid);
                return null;
            }));
        }

        Uni.join().all(unis).andFailFast()
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .await().atMost(Duration.ofSeconds(10));
    }

    @Test
    @Disabled("This test is flaky on CI, must be investigated")
    public void testThatBlockingRoutesAreCalledOnDuplicatedContext() {
        String uuid = UUID.randomUUID().toString();
        String resp = get("/context-blocking/" + uuid).asString();
        Assertions.assertEquals(resp, "OK-" + uuid);
    }

    @ApplicationScoped
    public static class MyRoutes {

        @Inject
        Vertx vertx;

        @Route(path = "/context/:id", methods = Route.HttpMethod.GET)
        void get(RoutingContext ctx) {
            Assertions.assertTrue(Thread.currentThread().getName().contains("vert.x-eventloop"));
            process(ctx);
        }

        @Route(path = "/context-blocking/:id", methods = Route.HttpMethod.GET)
        @Blocking
        void getBlocking(RoutingContext ctx) {
            Assertions.assertFalse(Thread.currentThread().getName().contains("vert.x-eventloop"));
            process(ctx);
        }

        private void process(RoutingContext ctx) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());

            String val = ContextLocals.<String> get("key").orElse(null);
            Assertions.assertNull(val);

            String id = ctx.pathParam("id");
            ContextLocals.put("key", id);

            vertx.createHttpClient().request(HttpMethod.GET, 8081, "localhost", "/hey")
                    .compose(request -> request.end().compose(x -> request.response()))
                    .compose(HttpClientResponse::body)
                    .map(Buffer::toString)
                    .onSuccess(msg -> {
                        Assertions.assertEquals("hey!", msg);
                        Assertions.assertEquals(id, ContextLocals.<String> get("key").orElseThrow());
                        Assertions.assertSame(Vertx.currentContext(), context);
                        ctx.response().end("OK-" + ContextLocals.get("key").orElseThrow());
                    });
        }

        @Route(path = "/hey", methods = Route.HttpMethod.GET)
        void hey(RoutingContext rc) {
            rc.response().end("hey!");
        }

    }

}
