package ilove.quark.us;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped 
public class MyDeclarativeRoutes {

    // neither path nor regex is set - match a path derived from the method name (ie => /hello )
    @Route(methods = Route.HttpMethod.GET) 
    void hello(RoutingContext rc) { 
        rc.response().end("Hello RESTEasy Reactive Route");
    }

    @Route(path = "/world")
    String helloWorld() { 
        return "Hello world !!";
    }

    @Route(path = "/greetings", methods = Route.HttpMethod.GET)
    void greetings(RoutingExchange ex) { 
        ex.ok("Hello  " + ex.getParam("name").orElse("RESTEasy Reactive Route") +" !!");
    }
}