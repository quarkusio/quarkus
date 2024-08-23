package io.quarkus.arc.test.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;

public class NullConverterTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(NullConverterBean.class)
                    .addAsServiceProvider(Converter.class, CustomTypeConverter.class)
                    .addAsResource(new StringAsset("my.prop=1234\n"), "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Test
    void nullProperty() {
        Assertions.fail();
    }

    @Unremovable
    @ApplicationScoped
    static class NullConverterBean {
        @Inject
        @ConfigProperty(name = "my.prop", defaultValue = "1234")
        CustomType customType;
    }

    static class CustomType {

    }

    public static class CustomTypeConverter implements Converter<CustomTypeConverter> {
        @Override
        public CustomTypeConverter convert(final String value)
                throws IllegalArgumentException, NullPointerException {
            return null;
        }
    }
}
