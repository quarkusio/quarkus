package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Set;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ResumingRouter implements Router {

    private final Router delegate;

    public ResumingRouter(Router delegate) {
        this.delegate = delegate;
    }

    @Override
    @Deprecated
    public void accept(HttpServerRequest request) {
        delegate.accept(request);
    }

    @Override
    public Route route() {
        return new ResumingRoute(delegate.route());
    }

    @Override
    public Route route(HttpMethod method, String path) {
        return new ResumingRoute(delegate.route(method, path));
    }

    @Override
    public Route route(String path) {
        return new ResumingRoute(delegate.route(path));
    }

    @Override
    public Route routeWithRegex(HttpMethod method, String regex) {
        return new ResumingRoute(delegate.routeWithRegex(method, regex));
    }

    @Override
    public Route routeWithRegex(String regex) {
        return new ResumingRoute(delegate.routeWithRegex(regex));
    }

    @Override
    public Route get() {
        return new ResumingRoute(delegate.get());
    }

    @Override
    public Route get(String path) {
        return new ResumingRoute(delegate.get(path));
    }

    @Override
    public Route getWithRegex(String regex) {
        return new ResumingRoute(delegate.getWithRegex(regex));
    }

    @Override
    public Route head() {
        return new ResumingRoute(delegate.head());
    }

    @Override
    public Route head(String path) {
        return new ResumingRoute(delegate.head(path));
    }

    @Override
    public Route headWithRegex(String regex) {
        return new ResumingRoute(delegate.headWithRegex(regex));
    }

    @Override
    public Route options() {
        return new ResumingRoute(delegate.options());
    }

    @Override
    public Route options(String path) {
        return new ResumingRoute(delegate.options(path));
    }

    @Override
    public Route optionsWithRegex(String regex) {
        return new ResumingRoute(delegate.optionsWithRegex(regex));
    }

    @Override
    public Route put() {
        return new ResumingRoute(delegate.put());
    }

    @Override
    public Route put(String path) {
        return new ResumingRoute(delegate.put(path));
    }

    @Override
    public Route putWithRegex(String regex) {
        return new ResumingRoute(delegate.putWithRegex(regex));
    }

    @Override
    public Route post() {
        return new ResumingRoute(delegate.post());
    }

    @Override
    public Route post(String path) {
        return new ResumingRoute(delegate.post(path));
    }

    @Override
    public Route postWithRegex(String regex) {
        return new ResumingRoute(delegate.postWithRegex(regex));
    }

    @Override
    public Route delete() {
        return new ResumingRoute(delegate.delete());
    }

    @Override
    public Route delete(String path) {
        return new ResumingRoute(delegate.delete(path));
    }

    @Override
    public Route deleteWithRegex(String regex) {
        return new ResumingRoute(delegate.deleteWithRegex(regex));
    }

    @Override
    public Route trace() {
        return new ResumingRoute(delegate.trace());
    }

    @Override
    public Route trace(String path) {
        return new ResumingRoute(delegate.trace(path));
    }

    @Override
    public Route traceWithRegex(String regex) {
        return new ResumingRoute(delegate.traceWithRegex(regex));
    }

    @Override
    public Route connect() {
        return new ResumingRoute(delegate.connect());
    }

    @Override
    public Route connect(String path) {
        return new ResumingRoute(delegate.connect(path));
    }

    @Override
    public Route connectWithRegex(String regex) {
        return new ResumingRoute(delegate.connectWithRegex(regex));
    }

    @Override
    public Route patch() {
        return new ResumingRoute(delegate.patch());
    }

    @Override
    public Route patch(String path) {
        return new ResumingRoute(delegate.patch(path));
    }

    @Override
    public Route patchWithRegex(String regex) {
        return delegate.patchWithRegex(regex);
    }

    @Override
    public List<Route> getRoutes() {
        return delegate.getRoutes();
    }

    @Override
    public Router clear() {
        return delegate.clear();
    }

    @Override
    public Router mountSubRouter(String mountPoint, Router subRouter) {
        return delegate.mountSubRouter(mountPoint, subRouter);
    }

    @Override
    @Deprecated
    public Router exceptionHandler(Handler<Throwable> exceptionHandler) {
        return delegate.exceptionHandler(exceptionHandler);
    }

    @Override
    public Router errorHandler(int statusCode, Handler<RoutingContext> errorHandler) {
        return delegate.errorHandler(statusCode, errorHandler);
    }

    @Override
    public void handleContext(RoutingContext context) {
        delegate.handleContext(context);
    }

    @Override
    public void handleFailure(RoutingContext context) {
        delegate.handleFailure(context);
    }

    @Override

    public Router modifiedHandler(Handler<Router> handler) {
        return delegate.modifiedHandler(handler);
    }

    @Override
    public void handle(HttpServerRequest event) {
        delegate.handle(event);
    }

    private static final class ResumingRoute implements Route {
        final Route route;

        private ResumingRoute(Route route) {
            this.route = route;
        }

        @Override
        public Route method(HttpMethod method) {
            return route.method(method);
        }

        @Override
        public Route path(String path) {
            return route.path(path);
        }

        @Override
        public Route pathRegex(String path) {
            return route.pathRegex(path);
        }

        @Override
        public Route produces(String contentType) {
            return route.produces(contentType);
        }

        @Override
        public Route consumes(String contentType) {
            return route.consumes(contentType);
        }

        @Override
        public Route order(int order) {
            return route.order(order);
        }

        @Override
        public Route last() {
            return route.last();
        }

        @Override
        public Route handler(Handler<RoutingContext> requestHandler) {
            return route.handler(new ResumeHandler(requestHandler));
        }

        @Override
        public Route blockingHandler(Handler<RoutingContext> requestHandler) {
            return route.blockingHandler(new ResumeHandler(requestHandler));
        }

        @Override
        public Route subRouter(Router subRouter) {
            return route.subRouter(subRouter);
        }

        @Override
        public Route blockingHandler(Handler<RoutingContext> requestHandler, boolean ordered) {
            return route.blockingHandler(new ResumeHandler(requestHandler), ordered);
        }

        @Override
        public Route failureHandler(Handler<RoutingContext> failureHandler) {
            return route.failureHandler(new ResumeHandler(failureHandler));
        }

        @Override
        public Route remove() {
            return route.remove();
        }

        @Override
        public Route disable() {
            return route.disable();
        }

        @Override
        public Route enable() {
            return route.enable();
        }

        @Override
        public Route useNormalisedPath(boolean useNormalisedPath) {
            return route.useNormalisedPath(useNormalisedPath);
        }

        @Override
        public String getPath() {
            return route.getPath();
        }

        @Override
        public boolean isRegexPath() {
            return route.isRegexPath();
        }

        @Override
        public Set<HttpMethod> methods() {
            return route.methods();
        }

        @Override
        public Route setRegexGroupsNames(List<String> groups) {
            return route.setRegexGroupsNames(groups);
        }
    }
}
