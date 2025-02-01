package io.quarkus.vertx.http.runtime.webjar;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Static handler for webjars. Delegates to either Vert.x {@link StaticHandler} if finalDestination starts with
 * META-INF, or otherwise to {@link FileSystemStaticHandler}.
 */
public class WebJarStaticHandler implements Handler<RoutingContext>, Closeable {
    private static final ReentrantLock HANDLER_CREATION_LOCK = new ReentrantLock();

    private String finalDestination;

    private String path;

    private List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations;

    private Handler<RoutingContext> handler;

    public WebJarStaticHandler() {
    }

    public WebJarStaticHandler(String finalDestination, String path,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations) {
        this.finalDestination = finalDestination;
        this.path = path;
        this.webRootConfigurations = webRootConfigurations;
    }

    public String getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(String finalDestination) {
        this.finalDestination = finalDestination;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FileSystemStaticHandler.StaticWebRootConfiguration> getWebRootConfigurations() {
        return webRootConfigurations;
    }

    public void setWebRootConfigurations(List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations) {
        this.webRootConfigurations = webRootConfigurations;
    }

    @Override
    public void handle(RoutingContext event) {

        if (event.normalizedPath().length() == path.length()) {
            event.response().setStatusCode(302);
            event.response().headers().set(HttpHeaders.LOCATION, path + "/");
            event.response().end();
            return;
        } else if (event.normalizedPath().length() == path.length() + 1) {
            event.reroute(path + "/index.html");
            return;
        }

        if (handler == null) {
            try {
                HANDLER_CREATION_LOCK.lock();
                if (handler == null) {
                    if (finalDestination != null && finalDestination.startsWith("META-INF")) {
                        handler = StaticHandler.create(finalDestination)
                                .setDefaultContentEncoding("UTF-8");
                    } else if (webRootConfigurations != null) {
                        handler = new FileSystemStaticHandler(webRootConfigurations);
                    } else {
                        throw new RuntimeException("Could not determine which StaticHandler to create.");
                    }
                }
            } finally {
                HANDLER_CREATION_LOCK.unlock();
            }
        }

        handler.handle(event);
    }

    @Override
    public void close() throws IOException {
        if (handler instanceof Closeable) {
            ((Closeable) handler).close();
        }
    }
}
