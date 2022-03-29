package io.quarkus.vertx.http.cdi;

import static io.restassured.RestAssured.given;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpListeningEventTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Observer.class));

    @ConfigProperty(name = "quarkus.http.test-port")
    int expectedPort;

    @Test
    public void test() throws Exception {
        given().get("/hello").then().statusCode(200)
                .body(Matchers.equalTo("hello from port " + expectedPort));
    }

    @ApplicationScoped
    static class Observer {

        private volatile int port;

        public void observeListening(@Observes VertxHttpListeningEvent event) {
            port = event.getHttpServerOptions().getPort();
        }

        public void registerHandler(@Observes Router router) {
            router.route("/hello").handler(new Handler<>() {
                @Override
                public void handle(RoutingContext event) {
                    event.end("hello from port " + port);
                }
            });

        }

    }
}
