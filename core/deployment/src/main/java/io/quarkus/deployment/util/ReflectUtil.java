package io.quarkus.deployment.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.deployment.annotations.BuildProducer;

/**
 */
public final class ReflectUtil {
    private ReflectUtil() {
    }

    public static boolean rawTypeIs(Type type, Class<?> clazz) {
        return type instanceof Class<?> && clazz == type
                || type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == clazz
                || type instanceof GenericArrayType && clazz.isArray()
                        && rawTypeIs(((GenericArrayType) type).getGenericComponentType(), clazz.getComponentType());
    }

    public static boolean rawTypeExtends(Type type, Class<?> clazz) {
        return type instanceof Class<?> && clazz.isAssignableFrom((Class<?>) type)
                || type instanceof ParameterizedType && rawTypeExtends(((ParameterizedType) type).getRawType(), clazz)
                || type instanceof GenericArrayType
                        && rawTypeExtends(((GenericArrayType) type).getGenericComponentType(), clazz.getComponentType());
    }

    public static boolean isListOf(Type type, Class<?> nestedType) {
        return isThingOf(type, List.class, nestedType);
    }

    public static boolean isConsumerOf(Type type, Class<?> nestedType) {
        return isThingOf(type, Consumer.class, nestedType);
    }

    public static boolean isBuildProducerOf(Type type, Class<?> nestedType) {
        return isThingOf(type, BuildProducer.class, nestedType);
    }

    public static boolean isSupplierOf(Type type, Class<?> nestedType) {
        return isThingOf(type, Supplier.class, nestedType);
    }

    public static boolean isSupplierOfOptionalOf(Type type, Class<?> nestedType) {
        return type instanceof ParameterizedType && rawTypeIs(type, Supplier.class)
                && isOptionalOf(typeOfParameter(type, 0), nestedType);
    }

    public static boolean isOptionalOf(Type type, Class<?> nestedType) {
        return isThingOf(type, Optional.class, nestedType);
    }

    public static boolean isThingOf(Type type, Class<?> thing, Class<?> nestedType) {
        return type instanceof ParameterizedType && rawTypeIs(type, thing)
                && rawTypeExtends(typeOfParameter(type, 0), nestedType);
    }

    public static Class<?> rawTypeOf(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return rawTypeOf(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            return Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        } else {
            throw new IllegalArgumentException("Type has no raw type class: " + type);
        }
    }

    public static Type typeOfParameter(final Type type, final int paramIdx) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments()[paramIdx];
        } else {
            throw new IllegalArgumentException("Type is not parameterized: " + type);
        }
    }

    public static Class<?> rawTypeOfParameter(final Type type, final int paramIdx) {
        return rawTypeOf(typeOfParameter(type, paramIdx));
    }

    public static void setFieldVal(Field field, Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public static InstantiationError toError(final InstantiationException e) {
        final InstantiationError error = new InstantiationError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }

    public static IllegalAccessError toError(final IllegalAccessException e) {
        final IllegalAccessError error = new IllegalAccessError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }

    public static NoSuchMethodError toError(final NoSuchMethodException e) {
        final NoSuchMethodError error = new NoSuchMethodError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }

    public static NoSuchFieldError toError(final NoSuchFieldException e) {
        final NoSuchFieldError error = new NoSuchFieldError(e.getMessage());
        error.setStackTrace(e.getStackTrace());
        return error;
    }
}
