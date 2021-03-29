package io.quarkus.vertx.http.runtime.devmode;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public class DevConsoleStaticHandler implements Handler<RoutingContext> {

    private String devConsoleFinalDestination;
    private volatile StaticHandler staticHandler;

    public DevConsoleStaticHandler() {
    }

    public DevConsoleStaticHandler(String devConsoleFinalDestination) {
        this.devConsoleFinalDestination = devConsoleFinalDestination;
    }

    public String getDevConsoleFinalDestination() {
        return devConsoleFinalDestination;
    }

    public void setDevConsoleFinalDestination(String devConsoleFinalDestination) {
        this.devConsoleFinalDestination = devConsoleFinalDestination;
    }

    @Override
    public void handle(RoutingContext event) {
        getStaticHandler().handle(event);
    }

    private StaticHandler getStaticHandler() {
        if (staticHandler != null) {
            return staticHandler;
        }

        synchronized (this) {
            StaticHandler localStaticHandler = staticHandler;
            if (localStaticHandler == null) {
                localStaticHandler = StaticHandler.create().setAllowRootFileSystemAccess(true)
                        .setWebRoot(devConsoleFinalDestination)
                        .setDefaultContentEncoding("UTF-8");
                staticHandler = localStaticHandler;
            }

            return localStaticHandler;
        }
    }
}
