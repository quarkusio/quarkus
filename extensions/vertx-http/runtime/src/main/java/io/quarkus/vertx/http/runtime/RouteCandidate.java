package io.quarkus.vertx.http.runtime;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * Creates {@link Function<Router, Route>} if {@link #path} underlying value
 * is not null during runtime init.
 */
public class RouteCandidate implements Function<Router, Route> {

    private Supplier<String> path;

    public RouteCandidate() {
    }

    public RouteCandidate(Supplier<String> path) {
        Objects.requireNonNull(path);
        this.path = path;
    }

    public Supplier<String> getPath() {
        return path;
    }

    public void setPath(Supplier<String> path) {
        this.path = path;
    }

    /* RUNTIME_INIT */
    @Override
    public Route apply(Router router) {
        final String resolvedPath = path.get();
        if (resolvedPath == null) {
            return null;
        }
        return router.route(resolvedPath);
    }
}
