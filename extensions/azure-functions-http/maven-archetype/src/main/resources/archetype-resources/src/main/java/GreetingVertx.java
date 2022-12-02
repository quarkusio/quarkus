package ${package};

import static io.quarkus.vertx.web.Route.HttpMethod.*;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class GreetingVertx {
    @Route(path = "/vertx/hello", methods = GET)
    void hello(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello vertx");
    }
}
