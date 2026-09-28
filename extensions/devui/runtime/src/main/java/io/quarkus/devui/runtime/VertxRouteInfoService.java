package io.quarkus.devui.runtime;

import java.lang.reflect.Method;
import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouteImpl;

@ApplicationScoped
public class VertxRouteInfoService {

    private static final Logger LOG = Logger.getLogger(VertxRouteInfoService.class);

    private Router router;
    private RouteStateAccess stateAccess;

    public void init(@Observes Router router) {
        this.router = router;
        // The order and the handlers are only available on the route state, which Vert.x does not expose publicly.
        // Everything else comes from the public Route API.
        // TODO: Remove the reflection once https://github.com/vert-x3/vertx-web/issues/2952 is available
        this.stateAccess = RouteStateAccess.create();
    }

    // There must be a better way to get this...
    public JsonArray getInfo() {
        JsonArray allRoutes = new JsonArray();
        for (Route route : router.getRoutes()) {
            JsonArray methods = new JsonArray();
            if (route.methods() != null) {
                route.methods().stream().map(HttpMethod::name).sorted().forEach(methods::add);
            }
            JsonObject routeInfo = new JsonObject()
                    .put("path", route.getPath())
                    .put("name", route.getName())
                    .put("methods", methods)
                    .put("exactPath", route.isExactPath())
                    .put("regexPath", route.isRegexPath())
                    .put("subRouter", route.getSubRouter() != null);
            if (stateAccess != null) {
                stateAccess.addTo(route, routeInfo);
            }
            allRoutes.add(routeInfo);
        }
        return allRoutes;
    }

    private record RouteStateAccess(Method state, Method order, Method contextHandlers, Method failureHandlers) {

        static RouteStateAccess create() {
            try {
                Method state = accessible(RouteImpl.class.getDeclaredMethod("state"));
                Class<?> stateClass = state.getReturnType();
                return new RouteStateAccess(state,
                        accessible(stateClass.getMethod("getOrder")),
                        accessible(stateClass.getMethod("getContextHandlers")),
                        accessible(stateClass.getMethod("getFailureHandlers")));
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.debug("Could not access the Vert.x route state, route handlers and order will not be shown", e);
                return null;
            }
        }

        void addTo(Route route, JsonObject routeInfo) {
            if (!(route instanceof RouteImpl)) {
                return;
            }
            try {
                Object routeState = state.invoke(route);
                routeInfo.put("order", order.invoke(routeState))
                        .put("contextHandlers", handlerClassNames(contextHandlers.invoke(routeState)))
                        .put("failureHandlers", handlerClassNames(failureHandlers.invoke(routeState)));
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.debug("Could not read the Vert.x route state of " + route.getPath(), e);
            }
        }

        private static JsonArray handlerClassNames(Object handlers) {
            JsonArray classNames = new JsonArray();
            if (handlers instanceof Collection<?> collection) {
                for (Object handler : collection) {
                    String className = handler.getClass().getName();
                    // Lambdas and method references are named after the class that defines them
                    int lambda = className.indexOf("$$Lambda");
                    classNames.add(lambda > 0 ? className.substring(0, lambda) : className);
                }
            }
            return classNames;
        }

        private static Method accessible(Method method) {
            method.setAccessible(true);
            return method;
        }
    }
}
