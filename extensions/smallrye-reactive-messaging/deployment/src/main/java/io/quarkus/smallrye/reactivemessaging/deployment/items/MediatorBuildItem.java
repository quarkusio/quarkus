package io.quarkus.smallrye.reactivemessaging.deployment.items;

import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a method annotated with {@code @Incoming} or / and {@code Outgoing}.
 */
public final class MediatorBuildItem extends MultiBuildItem {

    private final BeanInfo bean;

    private final MethodInfo method;

    public MediatorBuildItem(BeanInfo bean, MethodInfo method) {
        this.bean = bean;
        this.method = method;
    }

    public BeanInfo getBean() {
        return bean;
    }

    public MethodInfo getMethod() {
        return method;
    }

}
