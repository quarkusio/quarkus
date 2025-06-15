package io.quarkus.vertx.web.deployment;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.HttpCompression;

public final class AnnotatedRouteHandlerBuildItem extends MultiBuildItem {

    private final BeanInfo bean;
    private final List<AnnotationInstance> routes;
    private final AnnotationInstance routeBase;
    private final MethodInfo method;
    private final boolean blocking;
    private final HttpCompression compression;
    /**
     * If true, always attempt to authenticate user right before the body handler is run
     */
    private final boolean alwaysAuthenticateRoute;

    public AnnotatedRouteHandlerBuildItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> routes,
            AnnotationInstance routeBase) {
        this(bean, method, routes, routeBase, false, HttpCompression.UNDEFINED, false);
    }

    public AnnotatedRouteHandlerBuildItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> routes,
            AnnotationInstance routeBase, boolean blocking, HttpCompression compression,
            boolean alwaysAuthenticateRoute) {
        super();
        this.bean = bean;
        this.routes = routes;
        this.routeBase = routeBase;
        this.method = method;
        this.blocking = blocking;
        this.compression = compression;
        this.alwaysAuthenticateRoute = alwaysAuthenticateRoute;
    }

    public BeanInfo getBean() {
        return bean;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public List<AnnotationInstance> getRoutes() {
        return routes;
    }

    public AnnotationInstance getRouteBase() {
        return routeBase;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean shouldAlwaysAuthenticateRoute() {
        return alwaysAuthenticateRoute;
    }

    public HttpCompression getCompression() {
        return compression;
    }

}
