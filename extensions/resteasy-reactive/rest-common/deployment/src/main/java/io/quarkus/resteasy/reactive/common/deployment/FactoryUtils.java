package io.quarkus.resteasy.reactive.common.deployment;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.common.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;

public class FactoryUtils {
    public static <T> BeanFactory<T> factory(ClassInfo providerClass, Set<String> singletons,
            ResteasyReactiveCommonRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem) {
        return factory(providerClass.name().toString(), singletons, recorder, beanContainerBuildItem);
    }

    public static <T> BeanFactory<T> factory(String providerClass, Set<String> singletons,
            ResteasyReactiveCommonRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem) {
        Objects.requireNonNull(providerClass, "providerClass cannot be null");
        if (singletons.contains(providerClass)) {
            return new SingletonBeanFactory<>(providerClass);
        } else {
            return recorder.factory(providerClass,
                    beanContainerBuildItem.getValue());
        }
    }

    /**
     * Same as {@link #factory(String, Set, ResteasyReactiveCommonRecorder, BeanContainerBuildItem)} but classes that
     * are known not to be CDI beans are instantiated reflectively without asking the CDI container at runtime.
     */
    public static <T> BeanFactory<T> factory(String providerClass, Set<String> singletons,
            ResteasyReactiveCommonRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem, ValidationPhaseBuildItem validationPhase) {
        Objects.requireNonNull(providerClass, "providerClass cannot be null");
        if (singletons.contains(providerClass)) {
            return new SingletonBeanFactory<>(providerClass);
        }
        return factory(providerClass, recorder, beanContainerBuildItem, validationPhase);
    }

    /**
     * Returns a factory backed by the CDI container if a bean (or a bean removed as unused, so that ArC can still
     * report the programmatic lookup problem) has the given class among its types, and a plain reflective factory
     * otherwise.
     */
    public static <T> BeanFactory<T> factory(String providerClass,
            ResteasyReactiveCommonRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem, ValidationPhaseBuildItem validationPhase) {
        Objects.requireNonNull(providerClass, "providerClass cannot be null");
        if (mayBeBean(providerClass, validationPhase)) {
            return recorder.factory(providerClass, beanContainerBuildItem.getValue());
        }
        return recorder.reflectiveFactory(providerClass);
    }

    private static boolean mayBeBean(String className, ValidationPhaseBuildItem validationPhase) {
        DotName name = DotName.createSimple(className);
        return !validationPhase.getContext().beans().withBeanType(name).isEmpty()
                || !validationPhase.getContext().removedBeans().withBeanType(name).isEmpty();
    }
}
