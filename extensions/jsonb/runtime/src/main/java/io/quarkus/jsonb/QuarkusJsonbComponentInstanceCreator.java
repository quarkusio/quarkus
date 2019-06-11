package io.quarkus.jsonb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.yasson.spi.JsonbComponentInstanceCreator;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class QuarkusJsonbComponentInstanceCreator implements JsonbComponentInstanceCreator {

    private final Map<Class<?>, Object> components;

    // Instance handles are used to destroy the CDI-based components correctly
    private final List<InstanceHandle<?>> beanHandles;

    public QuarkusJsonbComponentInstanceCreator() {
        this.beanHandles = new ArrayList<>();
        this.components = new ConcurrentHashMap<>();
    }

    @Override
    public void close() throws IOException {
        beanHandles.forEach(InstanceHandle::close);
        beanHandles.clear();
        components.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOrCreateComponent(Class<T> componentClass) {
        return (T) components.computeIfAbsent(componentClass, c -> {
            InstanceHandle<T> beanHandle = Arc.container().instance(componentClass);
            if (beanHandle.isAvailable()) {
                beanHandles.add(beanHandle);
                return beanHandle.get();
            }
            try {
                return componentClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Cannot instantiate Jsonb component: " + componentClass, e);
            }
        });
    }

}
