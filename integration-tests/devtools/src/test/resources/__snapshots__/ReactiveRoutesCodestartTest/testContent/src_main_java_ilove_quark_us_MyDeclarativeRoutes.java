package ilove.quark.us;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped 
public class MyDeclarativeRoutes {
    // neither path nor regex is set - match a path derived from the method name (ie helloRoute => /hello-route )
    @Route(methods = Route.HttpMethod.GET)
    void helloRoute(RoutingExchange ex) { 
        ex.ok("Hello " + ex.getParam("name").orElse("Reactive Route") +" !!");
    }
}