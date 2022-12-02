package ilove.quark.us

import io.quarkus.vertx.web.Route
import io.quarkus.vertx.web.RoutingExchange
import io.vertx.ext.web.RoutingContext
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class MyDeclarativeRoutes {

    // neither path nor regex is set - match a path derived from the method name (ie helloRoute => /hello-route )
    @Route(methods = [Route.HttpMethod.GET])
    fun helloRoute(ex : RoutingExchange) {
       return ex.ok("Hello " + ex.getParam("name").orElse("Reactive Route") +" !!")
    }
}