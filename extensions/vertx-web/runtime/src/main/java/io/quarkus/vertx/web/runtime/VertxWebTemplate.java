package io.quarkus.vertx.web.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.vertx.web.Route;

@Template
public class VertxWebTemplate {

    public void configureRouter(BeanContainer container, Map<String, List<Route>> routeHandlers,
            VertxHttpConfiguration vertxHttpConfiguration, LaunchMode launchMode) {
        container.instance(HttpServerInitializer.class).initialize(vertxHttpConfiguration, routeHandlers, launchMode);
    }
}
