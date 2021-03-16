package io.quarkus.vertx.web.blocking;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpMethod;

public class BlockingRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyRoutes.class));

    @Test
    public void testBlockingRoutes() {
        get("/non-blocking")
                .then().statusCode(200)
                .body(containsString("nonBlocking-"), containsString("eventloop"));

        get("/blocking")
                .then().statusCode(200)
                .body(containsString("blocking-"), containsString("worker"));

        get("/worker")
                .then().statusCode(200)
                .body(containsString("worker-"), containsString("worker"));
    }

    @ApplicationScoped
    public static class MyRoutes {

        @Route(methods = HttpMethod.GET, path = "/non-blocking")
        public String nonBlocking() {
            return "nonBlocking-" + Thread.currentThread().getName();
        }

        @Route(methods = HttpMethod.GET, path = "/blocking", type = Route.HandlerType.BLOCKING)
        public String blocking() {
            return "blocking-" + Thread.currentThread().getName();
        }

        @Route(methods = HttpMethod.GET, path = "/worker")
        @Blocking
        public String worker() {
            return "worker-" + Thread.currentThread().getName();
        }

    }

}
