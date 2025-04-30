package io.quarkus.test.component.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.component.QuarkusComponentTestExtension;
import io.quarkus.test.component.TestConfigProperty;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigBuilderCustomizerTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .setConfigBuilderCustomizer(new Consumer<SmallRyeConfigBuilder>() {
                @Override
                public void accept(SmallRyeConfigBuilder builder) {
                    builder.withConverter(Boolean.class, 300, new CustomBooleanConverter());
                }
            }).build();

    @TestConfigProperty(key = "my.boolean", value = "jo")
    @Test
    public void testBuilderCustomizer(Foo foo) {
        assertTrue(foo.boolVal);
    }

    @Singleton
    public static class Foo {

        @ConfigProperty(name = "my.boolean")
        boolean boolVal;

    }

    @SuppressWarnings("serial")
    public static class CustomBooleanConverter implements Converter<Boolean> {

        @Override
        public Boolean convert(String value) throws IllegalArgumentException, NullPointerException {
            return "jo".equals(value) ? true : false;
        }

    }
}
