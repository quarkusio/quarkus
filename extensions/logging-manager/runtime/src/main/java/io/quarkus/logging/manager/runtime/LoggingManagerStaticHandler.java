package io.quarkus.logging.manager.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Handling static Logging Manager content
 */
public class LoggingManagerStaticHandler implements Handler<RoutingContext> {

    private String loggingManagerFinalDestination;
    private String loggingManagerPath;

    public LoggingManagerStaticHandler() {
    }

    public LoggingManagerStaticHandler(String loggingManagerFinalDestination, String loggingManagerPath) {
        this.loggingManagerFinalDestination = loggingManagerFinalDestination;
        this.loggingManagerPath = loggingManagerPath;
    }

    public String getLoggingManagerFinalDestination() {
        return loggingManagerFinalDestination;
    }

    public void setLoggingManagerFinalDestination(String loggingManagerFinalDestination) {
        this.loggingManagerFinalDestination = loggingManagerFinalDestination;
    }

    public String getLoggingManagerPath() {
        return loggingManagerPath;
    }

    public void setLoggingManagerPath(String loggingManagerPath) {
        this.loggingManagerPath = loggingManagerPath;
    }

    @Override
    public void handle(RoutingContext event) {
        StaticHandler staticHandler = StaticHandler.create().setAllowRootFileSystemAccess(true)
                .setWebRoot(loggingManagerFinalDestination)
                .setDefaultContentEncoding("UTF-8");

        if (event.normalisedPath().length() == loggingManagerPath.length()) {

            event.response().setStatusCode(302);
            event.response().headers().set(HttpHeaders.LOCATION, loggingManagerPath + "/");
            event.response().end();
            return;
        } else if (event.normalisedPath().length() == loggingManagerPath.length() + 1) {
            event.reroute(loggingManagerPath + "/index.html");
            return;
        }

        staticHandler.handle(event);
    }

}
