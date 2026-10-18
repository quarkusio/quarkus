package io.quarkus.vertx.web.deployment;

import static io.quarkus.arc.processor.Reproducibility.BEAN_COMPARATOR;
import static io.quarkus.arc.processor.Reproducibility.METHOD_COMPARATOR;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;

public final class AnnotatedRouteFilterBuildItem extends MultiBuildItem implements Comparable<AnnotatedRouteFilterBuildItem> {

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

    @Override
    public int compareTo(AnnotatedRouteFilterBuildItem other) {
        int result = BEAN_COMPARATOR.compare(bean, other.bean);
        if (result != 0) {
            return result;
        }
        return METHOD_COMPARATOR.compare(method, other.method);
    }

}
