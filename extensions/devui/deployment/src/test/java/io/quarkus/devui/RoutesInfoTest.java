package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class RoutesInfoTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClasses(SubRouterRoutes.class));

    @Test
    public void testRoutesWithSubRouter() {
        List<Map<String, Object>> routes = RestAssured.given()
                .when()
                .get("q/dev-ui/endpoints/routes.json")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        Map<String, Object> mount = findRoute(routes, "path", "/sub/");
        assertThat(mount.get("subRouter")).isEqualTo(true);
        assertThat(mount.get("order")).isInstanceOf(Integer.class);
        assertThat(mount.get("methods")).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
        assertThat(mount.get("contextHandlers")).asInstanceOf(InstanceOfAssertFactories.LIST)
                .first().asString().startsWith("io.vertx.ext.web").doesNotContain("$$Lambda");

        Map<String, Object> named = findRoute(routes, "name", "named-get");
        assertThat(named.get("path")).isEqualTo("/named");
        assertThat(named.get("methods")).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactly("GET");
        assertThat(named.get("contextHandlers")).asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(SubRouterRoutes.class.getName());

        // A route without a path must be sent as a JSON null, not as the string "null"
        Map<String, Object> pathless = findRoute(routes, "name", "pathless");
        assertThat(pathless).containsEntry("path", null);
    }

    private static Map<String, Object> findRoute(List<Map<String, Object>> routes, String key, String value) {
        return routes.stream()
                .filter(route -> value.equals(route.get(key)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No route with " + key + " " + value + " in " + routes));
    }

    @ApplicationScoped
    public static class SubRouterRoutes {

        @Inject
        Vertx vertx;

        void init(@Observes Router router) {
            // A sub-router prints all of its routes in its toString, which broke the route info
            Router subRouter = Router.router(vertx);
            subRouter.get("/a").handler(rc -> rc.response().end("a"));
            subRouter.get("/b").handler(rc -> rc.response().end("b"));
            router.route("/sub/*").subRouter(subRouter);
            router.get("/named").setName("named-get").handler(rc -> rc.response().end("named"));
            router.route().setName("pathless").handler(rc -> rc.next());
        }
    }
}
