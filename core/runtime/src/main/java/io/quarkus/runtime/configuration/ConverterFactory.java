package io.quarkus.runtime.configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.SmallRyeConfig;

/**
 * A factory to acquire a converter for a given type.
 *
 * @deprecated For removal once <a href="https://github.com/smallrye/smallrye-config/issues/80">SmallRye #80</a> is resolved.
 */
@Deprecated
public final class ConverterFactory {
    static final Method getConverter;

    static {
        try {
            getConverter = SmallRyeConfig.class.getDeclaredMethod("getConverter", Class.class);
            getConverter.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw reflectionFailure(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Converter<T> getConverter(final SmallRyeConfig smallRyeConfig, final Class<T> itemClass) {
        try {
            return (Converter<T>) getConverter.invoke(smallRyeConfig, itemClass);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw reflectionFailure(e);
        }
    }

    private static IllegalStateException reflectionFailure(final ReflectiveOperationException e) {
        return new IllegalStateException("Unexpected reflection failure", e);
    }
}
