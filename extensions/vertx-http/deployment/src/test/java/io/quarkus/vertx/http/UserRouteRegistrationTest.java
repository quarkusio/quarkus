package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class UserRouteRegistrationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanRegisteringRouteUsingObserves.class, BeanRegisteringRouteUsingInject.class));

    @Test
    public void test() {
        assertThat(RestAssured.get("/observes").asString()).isEqualTo("observers - ok");
        assertThat(RestAssured.get("/inject").asString()).isEqualTo("inject - ok");
        assertThat(given().body("test").contentType("text/plain").post("/body").asString()).isEqualTo("test");
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {
            router.route("/observes").handler(rc -> rc.response().end("observers - ok"));
        }

    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingInject {

        @Inject
        Router router;

        public void register(@Observes StartupEvent ignored) {
            router.route("/inject").handler(rc -> rc.response().end("inject - ok"));
            router.route().failureHandler(rc -> rc.failure().printStackTrace());
            router.route("/body").consumes("text/plain").handler(BodyHandler.create())
                    .handler(rc -> rc.response().end(rc.getBodyAsString()));
        }

    }
}
