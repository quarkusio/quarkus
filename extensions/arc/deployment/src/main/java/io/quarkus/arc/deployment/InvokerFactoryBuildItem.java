package io.quarkus.arc.deployment;

import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.InvokerBuilder;
import io.quarkus.arc.processor.InvokerFactory;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Provides access to {@link InvokerFactory}. May only be used in the bean registration phase,
 * observer registration phase, and validation phase (basically, until ArC generates
 * the classes). Afterwards, any attempt to call {@link #createInvoker(BeanInfo, MethodInfo)}
 * throws an exception.
 */
public final class InvokerFactoryBuildItem extends SimpleBuildItem {
    private final BeanDeployment beanDeployment;

    public InvokerFactoryBuildItem(BeanDeployment beanDeployment) {
        this.beanDeployment = beanDeployment;
    }

    public InvokerBuilder createInvoker(BeanInfo bean, MethodInfo method) {
        // always call `BeanDeployment.getInvokerFactory()`, because that checks whether it's too late
        return beanDeployment.getInvokerFactory().createInvoker(bean, method);
    }
}
