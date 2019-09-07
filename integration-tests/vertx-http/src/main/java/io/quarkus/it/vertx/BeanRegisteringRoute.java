package io.quarkus.it.vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.vertx.ext.web.Router;

@ApplicationScoped
public class BeanRegisteringRoute {

    //    @Inject Router router;

    void init(@Observes Router router) {
        System.out.println("Got router: " + router);
        //        router.route("/my-path").handler(rc -> rc.response().end("OK"));
    }
}
