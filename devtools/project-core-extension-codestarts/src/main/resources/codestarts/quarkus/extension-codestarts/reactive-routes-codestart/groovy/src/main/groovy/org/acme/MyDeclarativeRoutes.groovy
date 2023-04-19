package org.acme

import groovy.transform.CompileStatic

import jakarta.enterprise.context.ApplicationScoped

import io.quarkus.vertx.web.Route
import io.quarkus.vertx.web.RoutingExchange

@CompileStatic
@ApplicationScoped 
class MyDeclarativeRoutes {
    // neither path nor regex is set - match a path derived from the method name (ie helloRoute => /hello-route )
    @Route(methods = Route.HttpMethod.GET)
    def helloRoute(RoutingExchange ex) { 
        ex.ok('Hello ' + ex.getParam('name').orElse('Reactive Route') + ' !!')
    }
}
