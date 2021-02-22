package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassWithMissingConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(Configured.class, DummyProperties.class))
            .assertException(e -> {
                assertEquals(DeploymentException.class, e.getClass());
                assertTrue(e.getMessage().contains("dummy.foo"));
            });

    @Test
    public void shouldNotBeInvoked() {
        // This method should not be invoked
        fail();
    }

    @ApplicationScoped
    public static class Configured {

        @Inject
        DummyProperties dummyProperties;

        public String getFoo() {
            return dummyProperties.getFoo();
        }
    }

    @ConfigProperties
    public static class DummyProperties {

        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }
}
