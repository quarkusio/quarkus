package io.quarkus.vertx.web.runtime.devmode;

import static io.quarkus.runtime.TemplateHtmlBuilder.adjustRoot;

import java.util.List;

import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Lists all routes when no route matches the path in the dev mode.
 */
public class ResourceNotFoundHandler implements Handler<RoutingContext> {

    private final String httpRoot;
    private final List<RouteDescription> routes;
    private final List<String> additionalEndpoints;

    public ResourceNotFoundHandler(String httpRoot, List<RouteDescription> routes,
            List<String> additionalEndpoints) {
        this.httpRoot = httpRoot;
        this.routes = routes;
        this.additionalEndpoints = additionalEndpoints;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder("404 - Resource Not Found", "", "No resources discovered");
        builder.resourcesStart("Reactive Routes");
        builder.resourceStart();
        for (RouteDescription route : routes) {
            builder.method(route.getHttpMethod(),
                    route.getPath() != null ? TemplateHtmlBuilder.adjustRoot(httpRoot, route.getPath()) : "/*");
            builder.listItem(route.getJavaMethod());
            if (route.getConsumes() != null) {
                builder.consumes(route.getConsumes());
            }
            if (route.getProduces() != null) {
                builder.produces(route.getProduces());
            }
            builder.methodEnd();
        }
        builder.resourceEnd();
        builder.resourcesEnd();

        if (!additionalEndpoints.isEmpty()) {
            builder.resourcesStart("Additional endpoints");
            for (String additionalEndpoint : additionalEndpoints) {
                builder.staticResourcePath(additionalEndpoint);
            }
            builder.resourcesEnd();
        }
        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "text/html; charset=utf-8")
                .end(builder.toString());
    }

}
