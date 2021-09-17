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
                    .addClasses(BeanRegisteringRouteUsingObserves.class,
                            BeanRegisteringRouteUsingObservesWithMutinyRouter.class,
                            BeanRegisteringRouteUsingInject.class,
                            BeanRegisteringRouteUsingInjectWithMutinyRouter.class));

    @Test
    public void test() {
        assertThat(RestAssured.get("/observes").asString()).isEqualTo("observers - ok");
        assertThat(RestAssured.get("/observes-mutiny").asString()).isEqualTo("observers mutiny - ok");
        assertThat(RestAssured.get("/inject").asString()).isEqualTo("inject - ok");
        assertThat(RestAssured.get("/inject-mutiny").asString()).isEqualTo("inject mutiny - ok");
        assertThat(given().body("test").contentType("text/plain").post("/body").asString()).isEqualTo("test");
        assertThat(given().body("test mutiny").contentType("text/plain").post("/body-mutiny").asString())
                .isEqualTo("test mutiny");

    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {
            router.route("/observes").handler(rc -> rc.response().end("observers - ok"));
        }

    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObservesWithMutinyRouter {

        public void register(@Observes io.vertx.mutiny.ext.web.Router router) {
            router.route("/observes-mutiny").handler(rc -> rc.response().endAndForget("observers mutiny - ok"));
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

    @ApplicationScoped
    static class BeanRegisteringRouteUsingInjectWithMutinyRouter {

        @Inject
        io.vertx.mutiny.ext.web.Router router;

        public void register(@Observes StartupEvent ignored) {
            router.route("/inject-mutiny").handler(rc -> rc.response().endAndForget("inject mutiny - ok"));
            router.route("/body-mutiny").consumes("text/plain").handler(io.vertx.mutiny.ext.web.handler.BodyHandler.create())
                    .handler(rc -> rc.response().endAndForget(rc.getBodyAsString()));
        }

    }
}
