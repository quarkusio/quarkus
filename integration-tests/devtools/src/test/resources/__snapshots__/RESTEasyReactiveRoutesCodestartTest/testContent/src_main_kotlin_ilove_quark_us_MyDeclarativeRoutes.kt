package ilove.quark.us

import io.quarkus.vertx.web.Route
import io.quarkus.vertx.web.RoutingExchange
import io.vertx.ext.web.RoutingContext
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ReactiveGreetingResource {

    // neither path nor regex is set - match a path derived from the method name (ie => /hello)
    @Route( methods = [Route.HttpMethod.GET])
    fun hello(rc : RoutingContext) {
        rc.response().end("Hello RESTEasy Reactive Route")
    }

    @Route( path = "/world")
    fun helloWorld() = "Hello world !!"

    @Route(path = "/greetings", methods = [Route.HttpMethod.GET])
    fun hello(ex : RoutingExchange) {
       return ex.ok("Hello  " + ex.getParam("name").orElse("RESTEasy Reactive Route") +" !!")
    }
}