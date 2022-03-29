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

    public AnnotatedRouteHandlerBuildItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> routes,
            AnnotationInstance routeBase) {
        this(bean, method, routes, routeBase, false, HttpCompression.UNDEFINED);
    }

    public AnnotatedRouteHandlerBuildItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> routes,
            AnnotationInstance routeBase, boolean blocking, HttpCompression compression) {
        super();
        this.bean = bean;
        this.routes = routes;
        this.routeBase = routeBase;
        this.method = method;
        this.blocking = blocking;
        this.compression = compression;
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

    public HttpCompression getCompression() {
        return compression;
    }

}
