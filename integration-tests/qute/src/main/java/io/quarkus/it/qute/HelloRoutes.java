package io.quarkus.it.qute;

import jakarta.inject.Inject;

import io.quarkus.qute.Template;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.http.HttpServerRequest;

public class HelloRoutes {

    @Inject
    Template hello;

    @Route(produces = "text/html")
    Multi<String> helloRoute(HttpServerRequest request) {
        return hello.data("name", request.getParam("name")).createMulti();
    }

}
