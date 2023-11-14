package io.quarkus.it.vertx.websessions;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class CounterEndpoint {
    @Route(path = "/counter", methods = Route.HttpMethod.GET)
    String counter(RoutingContext ctx) {
        Session session = ctx.session();
        Integer counter = session.get("counter");
        counter = counter == null ? 1 : counter + 1;
        session.put("counter", counter);
        return session.id() + "|" + counter;
    }

    @Route(path = "/check-sessions", methods = Route.HttpMethod.GET)
    void checkSessions(RoutingContext ctx) {
        Session session = ctx.session();
        ctx.end(session != null ? "OK" : "KO");
    }
}
