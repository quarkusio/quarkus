package io.quarkus.vertx.web.deployment;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;

public final class AnnotatedRouteHandlerBuildItem extends MultiBuildItem {

    private final BeanInfo bean;
    private final List<AnnotationInstance> routes;
    private final AnnotationInstance routeBase;
    private final MethodInfo method;

    public AnnotatedRouteHandlerBuildItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> routes,
            AnnotationInstance routeBase) {
        this.bean = bean;
        this.method = method;
        this.routes = routes;
        this.routeBase = routeBase;
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

}
