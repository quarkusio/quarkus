package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A test cases against {@link ConverterSupport} class.
 */
public class ConverterSupportTest {

    public static class MyPojoUno {
    }

    public static class MyPojoDuo {
    }

    public static class MyPojoUnoConverter implements Converter<MyPojoUno> {

        @Override
        public MyPojoUno convert(final String value) {
            return new MyPojoUno();
        }
    }

    @Priority(333)
    public static class MyPojoDuoConverter implements Converter<MyPojoDuo> {

        @Override
        public MyPojoDuo convert(final String value) {
            return new MyPojoDuo();
        }
    }

    static class TestConverterWrapper {

        final Class<?> type;
        final int priority;
        final Converter<?> converter;

        public TestConverterWrapper(Class<?> type, int priority, Converter<?> converter) {
            this.type = type;
            this.priority = priority;
            this.converter = converter;
        }
    }

    static class TestConfigBuilder implements ConfigBuilder {

        private final Map<Class<?>, TestConverterWrapper> wrappers = new HashMap<>();

        @Override
        public ConfigBuilder addDefaultSources() {
            // do nothing
            return null;
        }

        @Override
        public ConfigBuilder addDiscoveredSources() {
            // do nothing
            return null;
        }

        @Override
        public ConfigBuilder addDiscoveredConverters() {
            // do nothing
            return null;
        }

        @Override
        public ConfigBuilder forClassLoader(ClassLoader loader) {
            // do nothing
            return null;
        }

        @Override
        public ConfigBuilder withSources(ConfigSource... sources) {
            // do nothing
            return null;
        }

        @Override
        public ConfigBuilder withConverters(Converter<?>... converters) {
            // do nothing
            return null;
        }

        @Override
        public <T> ConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
            wrappers.put(type, new TestConverterWrapper(type, priority, converter));
            return null;
        }

        @Override
        public Config build() {
            // do nothing
            return null;
        }

        public TestConverterWrapper get(Class<?> type) {
            return wrappers.get(type);
        }
    }

    TestConfigBuilder builder;

    @BeforeEach
    public void setup() {
        builder = new TestConfigBuilder();
        ConverterSupport.populateConverters(builder);
    }

    @Test
    public void testAllConvertersLoaded() {
        assertNotNull(builder.get(MyPojoUno.class));
        assertNotNull(builder.get(MyPojoDuo.class));
    }

    @Test
    public void testCorrectTypesMapping() {
        assertSame(MyPojoUnoConverter.class, builder.get(MyPojoUno.class).converter.getClass());
        assertSame(MyPojoDuoConverter.class, builder.get(MyPojoDuo.class).converter.getClass());
    }

    @Test
    public void testPriorityDefaults() {
        assertTrue(builder.get(MyPojoUno.class).priority == 100);
    }

    @Test
    public void testPriorityFromAnnotation() {
        assertTrue(builder.get(MyPojoDuo.class).priority == 333);
    }
}
