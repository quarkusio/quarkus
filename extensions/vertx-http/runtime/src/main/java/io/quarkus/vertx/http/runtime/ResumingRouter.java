package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return new ResumingRoute(delegate.patchWithRegex(regex));
    }

    @Override
    public List<Route> getRoutes() {
        return Optional.ofNullable(delegate.getRoutes())
                .map(List::stream)
                .orElseGet(Stream::empty)
                .map(ResumingRoute::new)
                .collect(Collectors.toList());
    }

    @Override
    public Router clear() {
        delegate.clear();
        return this;
    }

    @Override
    public Router mountSubRouter(String mountPoint, Router subRouter) {
        delegate.mountSubRouter(mountPoint, subRouter);
        return this;
    }

    @Override
    @Deprecated
    public Router exceptionHandler(Handler<Throwable> exceptionHandler) {
        delegate.exceptionHandler(exceptionHandler);
        return this;
    }

    @Override
    public Router errorHandler(int statusCode, Handler<RoutingContext> errorHandler) {
        delegate.errorHandler(statusCode, errorHandler);
        return this;
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
        delegate.modifiedHandler(handler);
        return this;
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
            route.method(method);
            return this;
        }

        @Override
        public Route path(String path) {
            route.path(path);
            return this;
        }

        @Override
        public Route pathRegex(String path) {
            route.pathRegex(path);
            return this;
        }

        @Override
        public Route produces(String contentType) {
            route.produces(contentType);
            return this;
        }

        @Override
        public Route consumes(String contentType) {
            route.consumes(contentType);
            return this;
        }

        @Override
        public Route order(int order) {
            route.order(order);
            return this;
        }

        @Override
        public Route last() {
            route.last();
            return this;
        }

        @Override
        public Route handler(Handler<RoutingContext> requestHandler) {
            route.handler(new ResumeHandler(requestHandler));
            return this;
        }

        @Override
        public Route blockingHandler(Handler<RoutingContext> requestHandler) {
            route.blockingHandler(new ResumeHandler(requestHandler));
            return this;
        }

        @Override
        public Route subRouter(Router subRouter) {
            route.subRouter(subRouter);
            return this;
        }

        @Override
        public Route blockingHandler(Handler<RoutingContext> requestHandler, boolean ordered) {
            route.blockingHandler(new ResumeHandler(requestHandler), ordered);
            return this;
        }

        @Override
        public Route failureHandler(Handler<RoutingContext> failureHandler) {
            route.failureHandler(new ResumeHandler(failureHandler));
            return this;
        }

        @Override
        public Route remove() {
            route.remove();
            return this;
        }

        @Override
        public Route disable() {
            route.disable();
            return this;
        }

        @Override
        public Route enable() {
            route.enable();
            return this;
        }

        @Override
        public Route useNormalisedPath(boolean useNormalisedPath) {
            route.useNormalisedPath(useNormalisedPath);
            return this;
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
            route.setRegexGroupsNames(groups);
            return this;
        }
    }
}
