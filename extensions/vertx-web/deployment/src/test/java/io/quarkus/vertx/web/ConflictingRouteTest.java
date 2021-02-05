package io.quarkus.vertx.web;

import static io.restassured.RestAssured.get;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class ConflictingRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyRoutes.class));

    @Test
    public void testRouteConflict() {
        String neo = get("/conflict/neo").asString();
        Assertions.assertEquals("neo", neo);

        String me = get("/conflict/me").asString();
        Assertions.assertEquals("/me called", me);

    }

    @ApplicationScoped
    public static class MyRoutes {

        @Route(path = "/conflict/:id", methods = HttpMethod.GET, order = 2)
        void getAccount(RoutingContext ctx) {
            ctx.response().end(ctx.pathParam("id"));
        }

        @Route(path = "/conflict/me", methods = HttpMethod.GET, order = 1)
        void getCurrentUserAccount(RoutingContext ctx) {
            ctx.response().end("/me called");
        }

    }
}
