/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.enterprise.context.spi.CreationalContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

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

    private String name;

    private Consumer<MethodCreator> creatorConsumer;

    private Consumer<MethodCreator> destroyerConsumer;

    private final Map<String, Object> params;

    /**
     *
     * @param implClassName
     * @param beanDeployment
     * @param beanConsumer
     */
    BeanConfigurator(DotName implClassName, BeanDeployment beanDeployment, Consumer<BeanInfo> beanConsumer) {
        this.implClass = beanDeployment.getIndex().getClassByName(Objects.requireNonNull(implClassName));
        this.beanDeployment = beanDeployment;
        this.beanConsumer = beanConsumer;
        this.types = new HashSet<>();
        this.qualifiers = new HashSet<>();
        this.scope = BuiltinScope.DEPENDENT.getInfo();
        this.params = new HashMap<>();
        this.name = null;
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

    public BeanConfigurator<T> addType(DotName className) {
        this.types.add(Type.create(className, Kind.CLASS));
        return this;
    }

    public BeanConfigurator<T> addQualifier(DotName annotationName) {
        this.qualifiers.add(AnnotationInstance.create(annotationName, null, new AnnotationValue[] {}));
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

    public BeanConfigurator<T> name(String name) {
        this.name = name;
        return this;
    }

    public BeanConfigurator<T> alternativePriority(int priority) {
        this.alternativePriority = priority;
        return this;
    }

    public <U extends T> BeanConfigurator<U> creator(Class<? extends BeanCreator<U>> creatorClazz) {
        return creator(mc -> {
            // return new FooBeanCreator().create(context, params)
            // TODO verify, optimize, etc.
            ResultHandle paramsHandle = mc.readInstanceField(
                    FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                    mc.getThis());
            ResultHandle creatorHandle = mc.newInstance(MethodDescriptor.ofConstructor(creatorClazz));
            ResultHandle[] params = { mc.getMethodParam(0), paramsHandle };
            ResultHandle ret = mc.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(BeanCreator.class, "create", Object.class, CreationalContext.class, Map.class),
                    creatorHandle, params);
            mc.returnValue(ret);
        });
    }

    public <U extends T> BeanConfigurator<U> creator(Consumer<MethodCreator> methodCreatorConsumer) {
        this.creatorConsumer = methodCreatorConsumer;
        return cast(this);
    }

    public <U extends T> BeanConfigurator<U> destroyer(Class<? extends BeanDestroyer<U>> destroyerClazz) {
        return destroyer(mc -> {
            // new FooBeanDestroyer().destroy(instance, context, params)
            // TODO verify, optimize, etc.
            ResultHandle paramsHandle = mc.readInstanceField(
                    FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                    mc.getThis());
            ResultHandle destoyerHandle = mc.newInstance(MethodDescriptor.ofConstructor(destroyerClazz));
            ResultHandle[] params = { mc.getMethodParam(0), mc.getMethodParam(1), paramsHandle };
            mc.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(BeanDestroyer.class, "destroy", Void.class, Object.class, CreationalContext.class,
                            Map.class),
                    destoyerHandle, params);
            mc.returnValue(null);
        });
    }

    public <U extends T> BeanConfigurator<U> destroyer(Consumer<MethodCreator> methodCreatorConsumer) {
        this.destroyerConsumer = methodCreatorConsumer;
        return cast(this);
    }

    /**
     * Perform sanity checks and register the bean.
     */
    public void done() {
        // TODO sanity checks
        beanConsumer.accept(new BeanInfo.Builder().implClazz(implClass).beanDeployment(beanDeployment).scope(scope).types(types)
                .qualifiers(qualifiers)
                .alternativePriority(alternativePriority).name(name).creator(creatorConsumer).destroyer(destroyerConsumer)
                .params(params).build());
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
    }

}
