package io.quarkus.vertx.http.devmode;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Address;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.vertx.http.HttpServer;
import io.vertx.ext.web.Router;

/**
 * In dev mode, the HTTP server survives a restart, so it does not bind again, but the restarted application has a new
 * {@link ValueRegistry}. The values of the bound server, and the {@code vertx-http} {@link Address}, have to be
 * available after the restart too.
 */
public class AddressDevModeRestartTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClass(AddressEndpoint.class));

    @Test
    void boundValuesAvailableAfterRestart() {
        when().get("/address")
                .then()
                .statusCode(200)
                .body(equalTo("before restart 8080 8080 http://localhost:8080"));

        TEST.modifySourceFile(AddressDevModeRestartTest.class,
                source -> source.replace("before restart", "after restart"));

        when().get("/address")
                .then()
                .statusCode(200)
                .body(equalTo("after restart 8080 8080 http://localhost:8080"));
    }

    @ApplicationScoped
    public static class AddressEndpoint {
        @Inject
        @Named(HttpServer.VERTX_HTTP)
        Instance<Address> address;

        @Inject
        ValueRegistry valueRegistry;

        void addRoute(@Observes Router router) {
            router.get("/address").handler(rc -> rc.response().end("before restart"
                    + " " + address.get().inet().orElseThrow().getPort()
                    + " " + valueRegistry.get(HttpServer.HTTP_PORT)
                    + " " + valueRegistry.get(HttpServer.LOCAL_BASE_URI)));
        }
    }
}
