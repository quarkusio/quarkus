package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;

public class RemovedConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(RemovedConfigBean.class)
                    .addClass(RemovedConfigMapping.class));

    @Test
    void skipConfigValidation() {
        assertEquals(0, Arc.container().beanManager().getBeans(RemovedConfigBean.class).size());
        assertEquals(0, Arc.container().beanManager().getBeans(RemovedConfigMapping.class).size());
    }

    @ApplicationScoped
    public static class RemovedConfigBean {
        @Inject
        RemovedConfigMapping mapping;
    }

    @ConfigMapping
    public interface RemovedConfigMapping {
        String prop();
    }
}
