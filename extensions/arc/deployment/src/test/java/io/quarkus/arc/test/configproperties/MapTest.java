package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class MapTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(WithMap.class))
            .assertException(e -> {
                assertEquals(DeploymentException.class, e.getClass());
                assertTrue(e.getMessage().contains("config-mappings"));
            });

    @Test
    public void shouldNotBeInvoked() {
        // This method should not be invoked
        fail();
    }

    @ConfigProperties
    public static class WithMap {

        public String name;
        public Map<String, String> other;
    }
}
