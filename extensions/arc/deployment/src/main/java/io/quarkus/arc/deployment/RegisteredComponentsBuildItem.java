package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.stream.Stream;

import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.BeanStream;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.builder.item.SimpleBuildItem;

abstract class RegisteredComponentsBuildItem extends SimpleBuildItem {

    private final Collection<BeanInfo> beans;
    private final Collection<InjectionPointInfo> injectionPoints;
    private final Collection<ObserverInfo> observers;
    private final BeanResolver beanResolver;

    public RegisteredComponentsBuildItem(BeanDeployment beanDeployment) {
        this.beans = beanDeployment.getBeans();
        this.injectionPoints = beanDeployment.getInjectionPoints();
        this.observers = beanDeployment.getObservers();
        this.beanResolver = beanDeployment.getBeanResolver();
    }

    /**
     * @return the registered beans
     */
    public Collection<BeanInfo> geBeans() {
        return beans;
    }

    /**
     * @return the registered injection points
     */
    public Collection<InjectionPointInfo> getInjectionPoints() {
        return injectionPoints;
    }

    /**
     * @return the registered observers
     */
    public Collection<ObserverInfo> getObservers() {
        return observers;
    }

    /**
     * 
     * @return a convenient {@link Stream} wrapper that can be used to filter a set of beans
     */
    public BeanStream beanStream() {
        return new BeanStream(beans);
    }

    /**
     * The bean resolver can be used to apply the type-safe resolution rules.
     * 
     * @return the bean resolver
     */
    public BeanResolver getBeanResolver() {
        return beanResolver;
    }

}
