package io.quarkus.smallrye.graphql.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Handling static Health UI content
 */
public class SmallRyeGraphQLStaticHandler implements Handler<RoutingContext> {

    private String graphqlUiFinalDestination;
    private String graphqlUiPath;

    public SmallRyeGraphQLStaticHandler() {
    }

    public SmallRyeGraphQLStaticHandler(String graphqlUiFinalDestination, String graphqlUiPath) {
        this.graphqlUiFinalDestination = graphqlUiFinalDestination;
        this.graphqlUiPath = graphqlUiPath;
    }

    public String getGraphqlUiFinalDestination() {
        return graphqlUiFinalDestination;
    }

    public void setGraphqlUiFinalDestination(String graphqlUiFinalDestination) {
        this.graphqlUiFinalDestination = graphqlUiFinalDestination;
    }

    public String getGraphqlUiPath() {
        return graphqlUiPath;
    }

    public void setGraphqlUiPath(String graphqlUiPath) {
        this.graphqlUiPath = graphqlUiPath;
    }

    @Override
    public void handle(RoutingContext event) {
        StaticHandler staticHandler = StaticHandler.create().setAllowRootFileSystemAccess(true)
                .setWebRoot(graphqlUiFinalDestination)
                .setDefaultContentEncoding("UTF-8");

        if (event.normalizedPath().length() == graphqlUiPath.length()) {

            event.response().setStatusCode(302);
            event.response().headers().set(HttpHeaders.LOCATION, graphqlUiPath + "/");
            event.response().end();
            return;
        } else if (event.normalizedPath().length() == graphqlUiPath.length() + 1) {
            event.reroute(graphqlUiPath + "/index.html");
            return;
        }

        staticHandler.handle(event);
    }

}
