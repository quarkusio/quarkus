/*
 * Copyright 2019 Red Hat, Inc.
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

import io.quarkus.arc.ContextCreator;
import io.quarkus.arc.InjectableContext;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Custom context configurator.
 *
 * @author Martin Kouba
 */
public final class ContextConfigurator {

    private final Consumer<ContextConfigurator> configuratorConsumer;

    Class<? extends Annotation> scopeAnnotation;

    boolean isNormal;

    Function<MethodCreator, ResultHandle> creator;

    final Map<String, Object> params;

    ContextConfigurator(Class<? extends Annotation> scopeAnnotation, Consumer<ContextConfigurator> configuratorConsumer) {
        this.scopeAnnotation = Objects.requireNonNull(scopeAnnotation);
        this.params = new HashMap<>();
        this.configuratorConsumer = configuratorConsumer;
    }

    public ContextConfigurator param(String name, Class<?> value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, int value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, long value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, double value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, String value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, boolean value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator normal() {
        this.isNormal = true;
        return this;
    }

    public ContextConfigurator contextClass(Class<? extends InjectableContext> contextClazz) {
        return creator(mc -> mc.newInstance(MethodDescriptor.ofConstructor(contextClazz)));
    }

    public ContextConfigurator creator(Class<? extends ContextCreator> creatorClazz) {
        return creator(mc -> {
            ResultHandle paramsHandle = mc.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
            for (Entry<String, Object> entry : params.entrySet()) {
                ResultHandle valHandle = null;
                if (entry.getValue() instanceof String) {
                    valHandle = mc.load(entry.getValue().toString());
                } else if (entry.getValue() instanceof Integer) {
                    valHandle = mc.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class),
                            mc.load(((Integer) entry.getValue()).intValue()));
                } else if (entry.getValue() instanceof Long) {
                    valHandle = mc.newInstance(MethodDescriptor.ofConstructor(Long.class, long.class),
                            mc.load(((Long) entry.getValue()).longValue()));
                } else if (entry.getValue() instanceof Double) {
                    valHandle = mc.newInstance(MethodDescriptor.ofConstructor(Double.class, double.class),
                            mc.load(((Double) entry.getValue()).doubleValue()));
                } else if (entry.getValue() instanceof Class) {
                    valHandle = mc.loadClass((Class<?>) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    valHandle = mc.load((Boolean) entry.getValue());
                }
                mc.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, paramsHandle, mc.load(entry.getKey()), valHandle);
            }
            ResultHandle creatorHandle = mc.newInstance(MethodDescriptor.ofConstructor(creatorClazz));
            ResultHandle ret = mc.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(ContextCreator.class, "create", InjectableContext.class, Map.class),
                    creatorHandle, paramsHandle);
            return ret;
        });
    }

    public ContextConfigurator creator(Function<MethodCreator, ResultHandle> creator) {
        this.creator = creator;
        return this;
    }

    public void done() {
        Objects.requireNonNull(creator);
        Objects.requireNonNull(configuratorConsumer).accept(this);
    }

}
