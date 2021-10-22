package ilove.quark.us.googlecloudfunctionshttp;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class GreetingRoutes {
    @Route(path = "/vertx/hello", methods = GET)
    void hello(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello");
    }

    @Route(path = "/vertx/error", methods = GET)
    void error(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(500).end("Oups!");
    }
}
