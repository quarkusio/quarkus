package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class NestedClassConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class, DummyProperties.NestedDummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.nested.rock-of-ages=1,2,3,4\ndummy.unused=whatever\ndummy.nested.unused=whatever2"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals(4, dummyBean.getAge().size());
        assertTrue(dummyBean.getAge().contains(1));
        assertTrue(dummyBean.getAge().contains(2));
        assertTrue(dummyBean.getAge().contains(3));
        assertTrue(dummyBean.getAge().contains(4));
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;

        String getName() {
            return dummyProperties.getName();
        }

        Set<Integer> getAge() {
            return dummyProperties.nested.ages;
        }
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties {

        private String name;
        public NestedDummyProperties nested;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static class NestedDummyProperties {

            @ConfigProperty(name = "rock-of-ages")
            public Set<Integer> ages;
        }
    }
}
