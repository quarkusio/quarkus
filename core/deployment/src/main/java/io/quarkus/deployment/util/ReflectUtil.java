package io.quarkus.deployment.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
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

    private static final Class<?>[] NO_CLASSES = new Class[0];

    public static Class<?>[] rawTypesOfDestructive(final Type[] types) {
        if (types.length == 0) {
            return NO_CLASSES;
        }
        Type t;
        Class<?> r;
        for (int i = 0; i < types.length; i++) {
            t = types[i];
            r = rawTypeOf(t);
            if (r != t) {
                types[i] = r;
            }
        }
        return Arrays.copyOf(types, types.length, Class[].class);
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

    public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw toError(e);
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        } catch (NoSuchMethodException e) {
            throw toError(e);
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

    public static UndeclaredThrowableException unwrapInvocationTargetException(InvocationTargetException original) {
        try {
            throw original.getCause();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            return new UndeclaredThrowableException(t);
        }
    }

    public static IllegalArgumentException reportError(AnnotatedElement e, String fmt, Object... args) {
        if (e instanceof Member) {
            return new IllegalArgumentException(
                    String.format(fmt, args) + " at " + e + " of " + ((Member) e).getDeclaringClass());
        } else if (e instanceof Parameter) {
            return new IllegalArgumentException(
                    String.format(fmt, args) + " at " + e + " of " + ((Parameter) e).getDeclaringExecutable() + " of "
                            + ((Parameter) e).getDeclaringExecutable().getDeclaringClass());
        } else {
            return new IllegalArgumentException(String.format(fmt, args) + " at " + e);
        }
    }
}
