package org.jboss.resteasy.reactive.client.impl;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
public class ReflectionUtil {

    private ReflectionUtil() {
    }

    /**
     * Used by io.quarkus.resteasy.reactive.client.deployment.beanparam.FieldExtractor
     *
     * @param object object to read the field from
     * @param clazz class that declares the field
     * @param fieldName name of the field
     * @return value of the field
     */
    public static Object readField(Object object, Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(object);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Cannot read '" + fieldName + "' field from " + object + " of class " + clazz);
        }
    }
}
