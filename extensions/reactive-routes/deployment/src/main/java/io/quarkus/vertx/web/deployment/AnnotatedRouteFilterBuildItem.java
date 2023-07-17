package io.quarkus.vertx.web.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;

public final class AnnotatedRouteFilterBuildItem extends MultiBuildItem {

    private final BeanInfo bean;
    private final AnnotationInstance routeFilter;
    private final MethodInfo method;

    public AnnotatedRouteFilterBuildItem(BeanInfo bean, MethodInfo method, AnnotationInstance routeFilter) {
        this.bean = bean;
        this.method = method;
        this.routeFilter = routeFilter;
    }

    public BeanInfo getBean() {
        return bean;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public AnnotationInstance getRouteFilter() {
        return routeFilter;
    }

}
