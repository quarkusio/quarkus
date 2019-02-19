package io.quarkus.vertx.web.deployment;

import java.util.List;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;

public final class RouteHandlerBuildItem extends MultiBuildItem {

    private final BeanInfo bean;
    private final List<AnnotationInstance> routes;
    private final MethodInfo method;

    public RouteHandlerBuildItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> routes) {
        this.bean = bean;
        this.method = method;
        this.routes = routes;
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

}
