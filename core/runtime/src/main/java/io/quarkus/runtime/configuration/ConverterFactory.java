package io.quarkus.runtime.configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * A factory to acquire a converter for a given type.
 *
 * @deprecated For removal once <a href="https://github.com/smallrye/smallrye-config/issues/80">SmallRye #80</a> is resolved.
 */
@Deprecated
public final class ConverterFactory {

    static final Method getImplicitConverter = accessible(getImplicitConverterMethod());
    static final Method getConverterType = accessible(getConverterTypeMethod());
    static final SmallRyeConfigBuilder smallRyeConfigBuilder = new SmallRyeConfigBuilder();

    @SuppressWarnings("unchecked")
    public static <T> Converter<T> getImplicitConverter(final Class<T> itemClass) {
        try {
            return (Converter<T>) getImplicitConverter.invoke(null, itemClass);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw reflectionFailure(e);
        }
    }

    /**
     * This method will find what is the generic type of a given {@link Converter}. Currently it works by
     * reflective invocation of {@link SmallRyeConfigBuilder#getConverterType} method, but later it should
     * be changed to call it directly, after method from {@link SmallRyeConfigBuilder} becomes public.
     *
     * @param <T> the inferred converter type
     * @param converter the {@link Converter} to get resultant type from
     * @return Converter resultant type
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getConverterType(final Converter<T> converter) {
        try {
            return (Class<T>) getConverterType.invoke(smallRyeConfigBuilder, converter.getClass());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw reflectionFailure(e);
        }
    }

    private static IllegalStateException reflectionFailure(final ReflectiveOperationException e) {
        return new IllegalStateException("Unexpected reflection failure", e);
    }

    private static Method getImplicitConverterMethod() {
        try {
            return Class
                    .forName("io.smallrye.config.ImplicitConverters")
                    .getDeclaredMethod("getConverter", Class.class);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw reflectionFailure(e);
        }
    }

    private static Method getConverterTypeMethod() {
        try {
            return Class
                    .forName("io.smallrye.config.SmallRyeConfigBuilder")
                    .getDeclaredMethod("getConverterType", Class.class);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw reflectionFailure(e);
        }
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }
}
