package io.quarkus.vertx.web.reactive;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class RequestContextPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyRoutes.class, Ping.class));

    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        get("/ping?val=bar").then().statusCode(200).body(is("foo_bar"));
        assertTrue(Ping.DESTROYED.get(2, TimeUnit.SECONDS));
    }

    @Singleton
    static class MyRoutes {

        @Inject
        Ping ping;

        @Route(path = "ping")
        void ping(RoutingContext ctx) {
            // Init the Ping bean
            ping.init("foo_");
            Uni.createFrom().item(ctx.request().getParam("val"))
                    .onItem().delayIt().by(Duration.ofMillis(500))
                    .subscribe().with(
                            // The context should be propagated inside the mutiny callback
                            i -> ctx.response().end(ping.pong(i)));
        }

    }

    @RequestScoped
    static class Ping {

        static final CompletableFuture<Boolean> DESTROYED = new CompletableFuture<>();

        private volatile String prefix;

        void init(String prefix) {
            this.prefix = prefix;
        }

        String pong(String pong) {
            return prefix + pong;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.complete(true);
        }

    }
}
