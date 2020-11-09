package io.quarkus.resteasy.reactive.common.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.EndpointInvoker;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuarkusRestCommonRecorder {
    private static final Map<String, Class<?>> primitiveTypes;
    static {
        Map<String, Class<?>> prims = new HashMap<>();
        prims.put(byte.class.getName(), byte.class);
        prims.put(boolean.class.getName(), boolean.class);
        prims.put(char.class.getName(), char.class);
        prims.put(short.class.getName(), short.class);
        prims.put(int.class.getName(), int.class);
        prims.put(float.class.getName(), float.class);
        prims.put(double.class.getName(), double.class);
        prims.put(long.class.getName(), long.class);
        primitiveTypes = Collections.unmodifiableMap(prims);
    }

    public Supplier<EndpointInvoker> invoker(String baseName) {
        return new Supplier<EndpointInvoker>() {
            @Override
            public EndpointInvoker get() {
                try {
                    return (EndpointInvoker) loadClass(baseName).newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException("Unable to generate endpoint invoker", e);
                }

            }
        };
    }

    public <T> BeanFactory<T> factory(String targetClass, BeanContainer beanContainer) {
        return new ArcBeanFactory<>(loadClass(targetClass),
                beanContainer);
    }

    public void registerWriter(Serialisers serialisers, String entityClassName,
            ResourceWriter writer) {
        serialisers.addWriter(loadClass(entityClassName), writer);
    }

    public void registerReader(Serialisers serialisers, String entityClassName,
            ResourceReader reader) {
        serialisers.addReader(loadClass(entityClassName), reader);
    }

    protected static <T> Class<T> loadClass(String name) {
        if (primitiveTypes.containsKey(name)) {
            return (Class<T>) primitiveTypes.get(name);
        }
        try {
            return (Class<T>) Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
