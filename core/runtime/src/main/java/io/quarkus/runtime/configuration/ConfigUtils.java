package io.quarkus.runtime.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.StringUtil;

/**
 *
 */
public final class ConfigUtils {
    private static final Map<ConverterClassHolder, Converter<?>> EXPLICIT_RUNTIME_CONVERTERS_CACHE = new HashMap<>();

    private ConfigUtils() {
    }

    /**
     * This method replicates the logic of {@link SmallRyeConfig#getValues(String, Class, IntFunction)} for the given
     * default value string.
     *
     * @param config the config instance (must not be {@code null})
     * @param defaultValue the default value string (must not be {@code null})
     * @param itemType the item type class (must not be {@code null})
     * @param converterClass - The converter class to use
     * @param collectionFactory the collection factory (must not be {@code null})
     * @param <T> the item type
     * @param <C> the collection type
     * @return the collection (not {@code null})
     */
    public static <T, C extends Collection<T>> C getDefaults(SmallRyeConfig config, String defaultValue, Class<T> itemType,
            Class<? extends Converter<T>> converterClass,
            IntFunction<C> collectionFactory) {
        final String[] items = Arrays.stream(StringUtil.split(defaultValue)).filter(s -> !s.isEmpty()).toArray(String[]::new);
        final C collection = collectionFactory.apply(items.length);
        for (String item : items) {
            if (converterClass == null) {
                collection.add(config.convert(item, itemType));
            } else {
                final Converter<T> converter = getConverterOfType(itemType, converterClass);
                final String rawValue = config.convert(item, String.class);
                collection.add(converter.convert(rawValue));
            }
        }

        return collection;
    }

    /**
     * Retrieve the value of a given config name from Configuration object. Converter the value to an appropriate type using the
     * given converter.
     *
     * @param config - Configuration object (must not be {@code null})
     * @param configName - the property name (must not be {@code null})
     * @param objectType - the type of the object (must not be {@code null})
     * @param converterClass - The converter class to use
     * @return the value in appropriate type
     */
    public static <T> T getValue(SmallRyeConfig config, String configName, Class<T> objectType,
            Class<? extends Converter<T>> converterClass) {
        if (converterClass == null) {
            return config.getValue(configName, objectType);
        }

        final Converter<T> converter = getConverterOfType(objectType, converterClass);
        final String rawValue = config.getValue(configName, String.class);
        return converter.convert(rawValue);
    }

    /**
     * Retrieve the Optional value of a property represented by the given config name. Converter the value to an appropriate
     * type using the given converter.
     *
     * @param config - Configuration object (must not be {@code null})
     * @param configName - the property name (must not be {@code null})
     * @param objectType - the type of the object (must not be {@code null})
     * @param converterClass - The converter class to use
     * @return Optional value of appropriate type
     */
    public static <T> Optional<T> getOptionalValue(SmallRyeConfig config, String configName, Class<T> objectType,
            Class<? extends Converter<T>> converterClass) {
        if (converterClass == null) {
            return config.getOptionalValue(configName, objectType);
        }

        final Converter<T> converter = getConverterOfType(objectType, converterClass);
        final String rawValue = config.getValue(configName, String.class);
        return Optional.ofNullable(converter.convert(rawValue));
    }

    /**
     * Retrieve the value of a given config name from Configuration object. Converter the value to an appropriate type using the
     * given converter.
     *
     * @param config - Configuration object (must not be {@code null})
     * @param configName - the property name (must not be {@code null})
     * @param objectType - the type of the object (must not be {@code null})
     * @param converterClass - The converter class to use
     * @return the values in appropriate type
     */
    public static <T> ArrayList<T> getValues(SmallRyeConfig config, String configName, Class<T> objectType,
            Class<? extends Converter<T>> converterClass) {
        if (converterClass == null) {
            return config.getValues(configName, objectType, ArrayListFactory.getInstance());
        }

        final Converter<T> converter = getConverterOfType(objectType, converterClass);
        final ArrayList<String> rawValues = config.getValues(configName, String.class, ArrayListFactory.getInstance());
        return rawValues.parallelStream().map(converter::convert).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Converter the value to an appropriate type using the given converter.
     *
     * @param config - Configuration object (must not be {@code null})
     * @param value - the value (must not be {@code null})
     * @param objectType - the type of the object (must not be {@code null})
     * @param converterClass - The converter class to use
     * @return the value
     */
    public static <T> T convert(SmallRyeConfig config, String value, Class<T> objectType,
            Class<? extends Converter<T>> converterClass) {
        if (converterClass == null) {
            return config.convert(value, objectType);
        }

        final Converter<T> converter = getConverterOfType(objectType, converterClass);
        final String rawValue = config.convert(value, String.class);
        return converter.convert(rawValue);
    }

    private static <T> Converter<T> getConverterOfType(Class<T> type, Class<? extends Converter<T>> converterType) {
        @SuppressWarnings("unchecked")
        final Converter<T> converter = (Converter<T>) EXPLICIT_RUNTIME_CONVERTERS_CACHE
                .get(new ConverterClassHolder(type, converterType));
        if (converter != null) {
            return converter;
        }

        // build time converter no need to be cached
        return newConverterInstance(type, converterType);
    }

    public static <T> Converter<T> newConverterInstance(Class<T> type, Class<? extends Converter<T>> converterClass) {
        // todo: this gets cleaned up with the SmallRye Config update
        if (HyphenateEnumConverter.class.equals(converterClass)) {
            @SuppressWarnings("unchecked")
            final Converter<T> converter = new HyphenateEnumConverter(type);
            return converter;
        }

        try {
            return converterClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void populateExplicitRuntimeConverter(Class<?> typeClass, Class<? extends Converter<?>> converterType,
            Converter<?> converter) {
        final Class<?> type = getWrapperClass(typeClass);
        EXPLICIT_RUNTIME_CONVERTERS_CACHE.put(new ConverterClassHolder(type, converterType), converter);
    }

    private static Class<?> getWrapperClass(Class<?> type) {
        if (type == Integer.TYPE) {
            return Integer.class;
        }

        if (type == Long.TYPE) {
            return Long.class;
        }
        if (type == Boolean.TYPE) {
            return Boolean.class;
        }
        if (type == Float.TYPE) {
            return Float.class;
        }

        if (type == Double.TYPE) {
            return Double.class;
        }

        return type;
    }

}
