package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class RemovedConfigPropertyTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(RemovedConfigBean.class));

    @Test
    void skipConfigValidation() {
        assertEquals(0, Arc.container().beanManager().getBeans(RemovedConfigBean.class).size());
    }

    @ApplicationScoped
    public static class RemovedConfigBean {
        @ConfigProperty(name = "my.prop")
        String prop;
    }
}
