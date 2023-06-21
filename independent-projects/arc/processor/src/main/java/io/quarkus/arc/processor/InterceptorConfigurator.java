package io.quarkus.arc.processor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.InterceptionType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;

import io.quarkus.arc.InterceptorCreator;
import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;

/**
 * This construct is not thread-safe.
 */
public final class InterceptorConfigurator extends ConfiguratorBase<InterceptorConfigurator> {

    private final BeanDeployment beanDeployment;

    final InterceptionType type;
    final Set<TypeAndQualifiers> injectionPoints;
    final Set<AnnotationInstance> bindings;
    int priority;

    InterceptorConfigurator(BeanDeployment beanDeployment, InterceptionType type) {
        this.beanDeployment = beanDeployment;
        this.type = type;
        this.injectionPoints = new HashSet<>();
        this.bindings = new HashSet<>();
        this.priority = 1;
    }

    public InterceptorConfigurator priority(int priority) {
        this.priority = priority;
        return this;
    }

    public InterceptorConfigurator bindings(AnnotationInstance... bindings) {
        Collections.addAll(this.bindings, bindings);
        return this;
    }

    public InterceptorConfigurator addInjectionPoint(Type requiredType, AnnotationInstance... requiredQualifiers) {
        this.injectionPoints.add(new TypeAndQualifiers(requiredType,
                requiredQualifiers.length == 0 ? Set.of(AnnotationInstance.builder(Default.class).build())
                        : Set.of(requiredQualifiers)));
        return this;
    }

    public void creator(Class<? extends InterceptorCreator> creatorClass) {
        beanDeployment.addSyntheticInterceptor(new InterceptorInfo(creatorClass, beanDeployment, bindings,
                List.of(Injection.forSyntheticInterceptor(injectionPoints)), priority, type, params));
    }

}
