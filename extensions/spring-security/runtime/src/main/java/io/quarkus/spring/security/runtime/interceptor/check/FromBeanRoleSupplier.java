package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;

public class FromBeanRoleSupplier implements Supplier<String[]> {

    private final Class<?> beanClass;
    private final Field field;

    public FromBeanRoleSupplier(Class<?> beanClass, String fieldName) {
        this.beanClass = beanClass;
        try {
            //note that this is intiialized at static init time
            //so no reflection registration required
            this.field = beanClass.getField(fieldName);
            if (field.getType() != String.class) {
                throw new RuntimeException("Field was not of type String");
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] get() {
        Object instance = Arc.container().select(beanClass).get();
        if (instance instanceof ClientProxy) {
            instance = ((ClientProxy) instance).arc_contextualInstance();
        }
        try {
            return new String[] { (String) field.get(instance) };
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
