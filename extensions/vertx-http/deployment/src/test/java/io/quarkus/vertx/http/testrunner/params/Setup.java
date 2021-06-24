package io.quarkus.vertx.http.testrunner.params;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.vertx.ext.web.Router;

@ApplicationScoped
public class Setup {

    public void route(@Observes Router router) {
        router.route("/hello").handler(new HelloResource());
        router.route("/odd/:num").handler(new OddResource());
    }

}
