package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassWithNotAllowedMissingSetterConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.unused=whatever"),
                            "application.properties"))
            .assertException(e -> {
                assertEquals(IllegalArgumentException.class, e.getClass());
                assertTrue(e.getMessage().contains("numbers"));
            });

    @Test
    public void shouldNotBeInvoked() {
        // This method should not be invoked
        fail();
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties {

        private String name;
        private List<Integer> numbers;

        public void setName(String name) {
            this.name = name;
        }
    }
}
