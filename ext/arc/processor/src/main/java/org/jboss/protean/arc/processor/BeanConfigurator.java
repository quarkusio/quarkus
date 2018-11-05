package org.jboss.protean.arc.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.protean.arc.BeanCreator;
import org.jboss.protean.arc.BeanDestroyer;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

/**
 * Synthetic bean configurator. An alternative to {@link javax.enterprise.inject.spi.configurator.BeanConfigurator}.
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public final class BeanConfigurator<T> {

    private final Consumer<BeanInfo> beanConsumer;

    private final BeanDeployment beanDeployment;

    private final ClassInfo implClass;

    private final Set<Type> types;

    private final Set<AnnotationInstance> qualifiers;

    private ScopeInfo scope;

    private Integer alternativePriority;

    private Consumer<MethodCreator> creatorConsumer;

    private Consumer<MethodCreator> destroyerConsumer;

    private final Map<String, Object> params;

    /**
     *
     * @param implClass
     * @param beanDeployment
     * @param beanConsumer
     */
    BeanConfigurator(Class<T> implClass, BeanDeployment beanDeployment, Consumer<BeanInfo> beanConsumer) {
        this.beanDeployment = beanDeployment;
        this.beanConsumer = beanConsumer;
        this.implClass = beanDeployment.getIndex().getClassByName(DotName.createSimple(implClass.getName()));
        if (this.implClass == null) {
            // TODO we have a problem
            throw new IllegalArgumentException();
        }
        this.types = new HashSet<>();
        this.qualifiers = new HashSet<>();
        this.scope = ScopeInfo.DEPENDENT;
        this.params = new HashMap<>();
    }

    public BeanConfigurator<T> param(String name, Class<?> value) {
        params.put(name, value);
        return this;
    }

    public BeanConfigurator<T> param(String name, int value) {
        params.put(name, value);
        return this;
    }

    public BeanConfigurator<T> param(String name, long value) {
        params.put(name, value);
        return this;
    }

    public BeanConfigurator<T> param(String name, double value) {
        params.put(name, value);
        return this;
    }

    public BeanConfigurator<T> param(String name, String value) {
        params.put(name, value);
        return this;
    }

    public BeanConfigurator<T> param(String name, boolean value) {
        params.put(name, value);
        return this;
    }

    // TODO other supported param types

    public BeanConfigurator<T> types(Class<?>... types) {
        for (Class<?> type : types) {
            this.types.add(Type.create(DotName.createSimple(type.getName()), Kind.CLASS));
        }
        return this;
    }

    public BeanConfigurator<T> types(Type... types) {
        Collections.addAll(this.types, types);
        return this;
    }

    public BeanConfigurator<T> qualifiers(AnnotationInstance... qualifiers) {
        Collections.addAll(this.qualifiers, qualifiers);
        return this;
    }

    public BeanConfigurator<T> scope(ScopeInfo scope) {
        this.scope = scope;
        return this;
    }

    public BeanConfigurator<T> alternativePriority(int priority) {
        this.alternativePriority = priority;
        return this;
    }

    public BeanConfigurator<T> creator(Class<? extends BeanCreator<T>> creatorClazz) {
        return creator(mc -> {
            // return new FooBeanCreator().create(context, params)
            // TODO verify, optimize, etc.
            ResultHandle paramsHandle = mc.readInstanceField(FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                    mc.getThis());
            ResultHandle creatorHandle = mc.newInstance(MethodDescriptor.ofConstructor(creatorClazz));
            ResultHandle[] params = { mc.getMethodParam(0), paramsHandle };
            ResultHandle ret = mc.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(BeanCreator.class, "create", Object.class, CreationalContext.class, Map.class), creatorHandle, params);
            mc.returnValue(ret);
        });
    }

    public BeanConfigurator<T> creator(Consumer<MethodCreator> methodCreatorConsumer) {
        this.creatorConsumer = methodCreatorConsumer;
        return this;
    }

    public BeanConfigurator<T> destroyer(Class<? extends BeanDestroyer<T>> destroyerClazz) {
        return destroyer(mc -> {
            // new FooBeanDestroyer().destroy(instance, context, params)
            // TODO verify, optimize, etc.
            ResultHandle paramsHandle = mc.readInstanceField(FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                    mc.getThis());
            ResultHandle destoyerHandle = mc.newInstance(MethodDescriptor.ofConstructor(destroyerClazz));
            ResultHandle[] params = { mc.getMethodParam(0), mc.getMethodParam(1), paramsHandle };
            mc.invokeInterfaceMethod(MethodDescriptor.ofMethod(BeanDestroyer.class, "destroy", Void.class, Object.class, CreationalContext.class, Map.class),
                    destoyerHandle, params);
            mc.returnValue(null);
        });
    }

    public BeanConfigurator<T> destroyer(Consumer<MethodCreator> methodCreatorConsumer) {
        this.destroyerConsumer = methodCreatorConsumer;
        return this;
    }

    // TODO stereotypes?

    /**
     * Perform sanity checks and register the bean.
     */
    public void done() {
        // TODO sanity checks
        beanConsumer.accept(new BeanInfo.Builder().implClazz(implClass).beanDeployment(beanDeployment).scope(scope).types(types).qualifiers(qualifiers)
                .alternativePriority(alternativePriority).creator(creatorConsumer).destroyer(destroyerConsumer).params(params).build());
    }

}