package org.jboss.protean.arc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * TODO security
 *
 * @author Martin Kouba
 */
public final class Reflections {

    private Reflections() {
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null) {
                return findMethod(clazz.getSuperclass(), methodName, parameterTypes);
            }
            throw new IllegalArgumentException(e);
        }
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object[] args) {
        Constructor<?> constructor = findConstructor(clazz, parameterTypes);
        if (constructor != null) {
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            try {
                return constructor.newInstance(args);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException("Cannot invoke constructor: " + clazz.getName(), e);
            }
        }
        throw new RuntimeException("No " + clazz.getName() + "constructor found for params: " + Arrays.toString(parameterTypes));
    }

    public static Object readField(Class<?> clazz, String name, Object instance) {
        try {
            Field field = clazz.getDeclaredField(name);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(instance);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Cannot read field value: " + clazz.getName() + "#" + name, e);
        }
    }

    public static void writeField(Class<?> clazz, String name, Object instance, Object value) {
        try {
            Field field = clazz.getDeclaredField(name);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(instance, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Cannot set field value: " + clazz.getName() + "#" + name, e);
        }
    }

    public static Object invokeMethod(Class<?> clazz, String name, Class<?>[] paramTypes, Object instance, Object[] args) {
        try {
            Method method = clazz.getDeclaredMethod(name, paramTypes);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method.invoke(instance, args);
        } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            System.out.println("INSTANCE: " + instance);
            throw new RuntimeException("Cannot invoke method: " + clazz.getName() + "#" + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }

}
