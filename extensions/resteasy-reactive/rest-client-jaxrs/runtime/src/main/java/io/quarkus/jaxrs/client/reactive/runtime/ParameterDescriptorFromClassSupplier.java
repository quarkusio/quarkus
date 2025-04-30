package io.quarkus.jaxrs.client.reactive.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ParameterDescriptorFromClassSupplier
        implements Supplier<Map<String, ParameterDescriptorFromClassSupplier.ParameterDescriptor>> {

    private final Class clazz;
    private volatile Map<String, ParameterDescriptor> value;

    public ParameterDescriptorFromClassSupplier(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public Map<String, ParameterDescriptor> get() {
        if (value == null) {
            value = new HashMap<>();
            Class currentClass = clazz;
            while (currentClass != null && currentClass != Object.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    ParameterDescriptor descriptor = new ParameterDescriptor();
                    descriptor.annotations = field.getAnnotations();
                    descriptor.genericType = field.getGenericType();
                    value.put(field.getName(), descriptor);
                }

                for (Method method : currentClass.getDeclaredMethods()) {
                    ParameterDescriptor descriptor = new ParameterDescriptor();
                    descriptor.annotations = method.getAnnotations();
                    descriptor.genericType = method.getGenericReturnType();
                    value.put(method.getName(), descriptor);
                }

                currentClass = currentClass.getSuperclass();
            }
        }

        return value;
    }

    public static class ParameterDescriptor {
        public Annotation[] annotations;
        public Type genericType;
    }
}
