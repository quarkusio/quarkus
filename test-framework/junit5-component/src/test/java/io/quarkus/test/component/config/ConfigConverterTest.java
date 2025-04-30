package io.quarkus.test.component.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.config.ConfigConverterTest.CustomBooleanConverter;

@QuarkusComponentTest(configConverters = CustomBooleanConverter.class)
public class ConfigConverterTest {

    @TestConfigProperty(key = "my.boolean", value = "jo")
    @TestConfigProperty(key = "my.duration", value = "5s")
    @Test
    public void testConverters(Foo foo) {
        assertEquals(TimeUnit.SECONDS.toMillis(5), foo.durationVal.toMillis());
        assertTrue(foo.boolVal);
    }

    @Singleton
    public static class Foo {

        @ConfigProperty(name = "my.duration", defaultValue = "60s")
        Duration durationVal;

        @ConfigProperty(name = "my.boolean")
        boolean boolVal;

    }

    @SuppressWarnings("serial")
    @Priority(300)
    public static class CustomBooleanConverter implements Converter<Boolean> {

        @Override
        public Boolean convert(String value) throws IllegalArgumentException, NullPointerException {
            return "jo".equals(value) ? true : false;
        }

    }
}
