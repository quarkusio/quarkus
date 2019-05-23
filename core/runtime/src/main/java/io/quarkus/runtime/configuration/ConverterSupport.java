package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Logger;

/**
 * This small utility class is a tool which helps populating SmallRye {@link ConfigBuilder} with
 * {@link Converter} implementations loaded from {@link ServiceLoader}.
 */
public class ConverterSupport {

    private static final Logger LOG = Logger.getLogger(ConverterSupport.class);

    /**
     * A list of {@link ConverterItem} which will be loaded in static initialization. This needs
     * to be static so we can load {@link Converter} implementations without producing reflective
     * class build items in the deployment time. The {@link ConverterItem} instances uses generic
     * {@link Object} type to avoid typecast errors from complier.
     */
    private static final List<ConverterItem<Object>> CONVERTERS = getConverters();

    /**
     * Default {@link Converter} priority with value {@value #DEFAULT_SMALLRYE_CONVERTER_PRIORITY}
     * to be used for all discovered converters in case when no {@link Priority} annotation is
     * available on the converter class.
     */
    public static final int DEFAULT_SMALLRYE_CONVERTER_PRIORITY = 100;

    /**
     * Default {@link Converter} priority with value {@value #DEFAULT_QUARKUS_CONVERTER_PRIORITY} to
     * be used for all Quarkus converters. The reason why Quarkus priority is higher than a default
     * one, which is {@value #DEFAULT_SMALLRYE_CONVERTER_PRIORITY}, is because Quarkus converters
     * should be used even if some third-party JAR bundles its own converters, unless these 3rd
     * party converters priority is explicitly higher to override Quarkus ones. This way we can be
     * sure that things have a good chance of consistent interoperability.
     */
    public static final int DEFAULT_QUARKUS_CONVERTER_PRIORITY = 200;

    /**
     * Populates given {@link ConfigBuilder} with all {@link Converter} implementations loaded from
     * the {@link ServiceLoader}.
     *
     * @param builder the {@link ConfigBuilder}
     */
    public static void populateConverters(final ConfigBuilder builder) {
        CONVERTERS.forEach(addConverterTo(builder));
    }

    /**
     * Get {@link Converter} priority by looking for a {@link Priority} annotation which can be put
     * on the converter type. If no {@link Priority} annotation is found a default priority of
     * {@value #DEFAULT_SMALLRYE_CONVERTER_PRIORITY} is returned.
     *
     * @param converterClass
     * @return
     */
    static int getConverterPriority(final Class<? extends Converter<?>> converterClass) {
        return Optional
                .ofNullable(converterClass.getAnnotation(Priority.class))
                .map(Priority::value)
                .orElse(DEFAULT_SMALLRYE_CONVERTER_PRIORITY);
    }

    /**
     * Converts {@link Converter} instance into {@link ConverterItem}.
     *
     * @param <T> the converter conversion type
     * @param converter the converter instance
     * @return New {@link ConverterItem} which wraps given {@link Converter} and related metadata
     */
    @SuppressWarnings("unchecked")
    private static <T> ConverterItem<?> converterToItem(final Converter<T> converter) {
        final Class<? extends Converter<T>> converterClass = (Class<? extends Converter<T>>) converter.getClass();
        final Class<T> convertedType = ConverterFactory.getConverterType(converter);
        final int priority = getConverterPriority(converterClass);
        return new ConverterItem<T>(convertedType, converter, priority);
    }

    /**
     * @return A {@link List} of {@link ConverterItem} loaded from {@link ServiceLoader}
     */
    @SuppressWarnings("unchecked")
    private static List<ConverterItem<Object>> getConverters() {
        final List<ConverterItem<Object>> items = new ArrayList<>();
        for (Converter<Object> converter : ServiceLoader.load(Converter.class)) {
            items.add((ConverterItem<Object>) converterToItem(converter));
        }
        return items;
    }

    /**
     * Create a {@link Consumer} which consumes {@link ConverterItem} wrapping {@link Converter}
     * (and related metadata) and adds it to the given {@link ConfigBuilder}.
     *
     * @param <T> the {@link Converter} conversion type
     * @param builder
     * @return A {@link ConverterItem} {@link Consumer} which populates {@link ConfigBuilder}
     */
    private static <T> Consumer<ConverterItem<T>> addConverterTo(final ConfigBuilder builder) {
        return item -> {

            final Class<T> type = item.getConvertedType();
            final Converter<T> converter = item.getConverter();
            final int priority = item.getPriority();

            LOG.debugf("Pupulate SmallRye config builder with converter for %s of priority %s", type, priority);

            builder.withConverter(type, priority, converter);
        };
    }

    private ConverterSupport() {
        // this is utility class
    }

    /**
     * This class wraps {@link Converter} and related metadata, i.e. a {@link Converter} conversion
     * type and its priority.
     *
     * @param <T> the {@link Converter} conversion type
     */
    private static final class ConverterItem<T> {

        final Class<T> convertedType;
        final Converter<T> converter;
        final int priority;

        public ConverterItem(final Class<T> convertedType, final Converter<T> converter, final int priority) {
            this.convertedType = convertedType;
            this.converter = converter;
            this.priority = priority;
        }

        /**
         * @return {@link Converter} conversion type
         */
        public Class<T> getConvertedType() {
            return convertedType;
        }

        /**
         * @return A {@link Converter} this item wraps
         */
        public Converter<T> getConverter() {
            return converter;
        }

        /**
         * @return A {@link Converter} priority
         */
        public int getPriority() {
            return priority;
        }
    }
}
