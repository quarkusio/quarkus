package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class RemovedConfigPropertiesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(RemovedConfigBean.class)
                    .addClass(RemovedConfigProperties.class));

    @Test
    void skipConfigValidation() {
        assertEquals(0, Arc.container().beanManager().getBeans(RemovedConfigBean.class).size());
        assertEquals(0, Arc.container().beanManager().getBeans(RemovedConfigProperties.class).size());
    }

    @ApplicationScoped
    public static class RemovedConfigBean {
        @Inject
        @ConfigProperties
        RemovedConfigProperties properties;
    }

    @ConfigProperties
    public static class RemovedConfigProperties {
        @ConfigProperty(name = "my.prop")
        String prop;

        public String getProp() {
            return prop;
        }
    }
}
