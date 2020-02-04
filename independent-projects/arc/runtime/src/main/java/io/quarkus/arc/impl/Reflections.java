package io.quarkus.arc.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Neither the class nor its methods are considered a public API and should only be used internally.
 */
public final class Reflections {

    // Note that we intentionally do not use weak references for keys/values
    // The reason is that:
    // (1) ArC does not support multiple deployments on the classpath
    // (2) the caches are cleared when the container is shut down
    private static final ComputingCache<FieldKey, Field> FIELDS_CACHE = new ComputingCache<>(new Function<FieldKey, Field>() {
        @Override
        public Field apply(FieldKey key) {
            return findFieldInternal(key.clazz, key.fieldName);
        }
    });
    private static final ComputingCache<MethodKey, Method> METHODS_CACHE = new ComputingCache<>(
            new Function<MethodKey, Method>() {
                @Override
                public Method apply(MethodKey key) {
                    return findMethodInternal(key.clazz, key.methodName, key.parameterTypes);
                }
            });

    static void clearCaches() {
        FIELDS_CACHE.clear();
        METHODS_CACHE.clear();
    }

    private Reflections() {
    }

    /**
     * 
     * @param clazz
     * @param fieldName
     * @return the field declared in the class hierarchy
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        return FIELDS_CACHE.getValue(new FieldKey(clazz, fieldName));
    }

    private static Field findFieldInternal(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findFieldInternal(clazz.getSuperclass(), fieldName);
            }
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 
     * @param clazz
     * @param methodName
     * @param parameterTypes
     * @return the method declared in the class hierarchy
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return METHODS_CACHE.getValue(new MethodKey(clazz, methodName, parameterTypes));
    }

    private static Method findMethodInternal(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null) {
                return findMethodInternal(clazz.getSuperclass(), methodName, parameterTypes);
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
        throw new RuntimeException(
                "No " + clazz.getName() + "constructor found for params: " + Arrays.toString(parameterTypes));
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
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException("Cannot invoke method: " + clazz.getName() + "#" + name + " on " + instance, e);
        } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Cannot invoke method: " + clazz.getName() + "#" + name + " on " + instance, e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        if (type instanceof ParameterizedType) {
            if (((ParameterizedType) type).getRawType() instanceof Class<?>) {
                return (Class<T>) ((ParameterizedType) type).getRawType();
            }
        }
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            Type[] bounds = variable.getBounds();
            return getBound(bounds);
        }
        if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            return getBound(wildcard.getUpperBounds());
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Class<?> rawType = getRawType(genericArrayType.getGenericComponentType());
            if (rawType != null) {
                return (Class<T>) Array.newInstance(rawType, 0).getClass();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getBound(Type[] bounds) {
        if (bounds.length == 0) {
            return (Class<T>) Object.class;
        } else {
            return getRawType(bounds[0]);
        }
    }

    static final class MethodKey {

        final Class<?> clazz;
        final String methodName;
        final Class<?>[] parameterTypes;
        final int hashCode;

        public MethodKey(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(parameterTypes);
            result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
            result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
            this.hashCode = result;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MethodKey other = (MethodKey) obj;
            return Objects.equals(clazz, other.clazz) && Objects.equals(methodName, other.methodName)
                    && Arrays.equals(parameterTypes, other.parameterTypes);
        }

    }

    static final class FieldKey {

        final Class<?> clazz;
        final String fieldName;
        final int hashCode;

        public FieldKey(Class<?> clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
            final int prime = 31;
            int result = 1;
            result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
            result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
            this.hashCode = result;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            FieldKey other = (FieldKey) obj;
            return Objects.equals(clazz, other.clazz) && Objects.equals(fieldName, other.fieldName);
        }

    }

}
