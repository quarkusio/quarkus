package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigIgnore;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class NestedClassAndSuperclassConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class, DummyProperties.NestedDummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.lname=redhat\ndummy.name=quarkus\ndummy.nested.ages=1,2,3,4\ndummy.supernested.afraid-of-heights=100,200\ndummy.unused=whatever\ndummy.nested.unused=whatever2"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        DummyProperties dummyProperties = dummyBean.dummyProperties;

        assertEquals("quarkus", dummyProperties.getName());
        assertEquals("redhat", dummyProperties.getLastname());
        Set<Integer> ages = dummyProperties.nested.ages;
        assertEquals(4, ages.size());
        assertTrue(ages.contains(1));
        assertTrue(ages.contains(2));
        assertTrue(ages.contains(3));
        assertTrue(ages.contains(4));
        Set<Integer> heights = dummyProperties.getSupernested().heights;
        assertEquals(2, heights.size());
        assertTrue(heights.contains(100));
        assertTrue(heights.contains(200));
        assertNull(dummyProperties.getSupernested().ignored);
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties extends SuperDummyProperties {

        private String name;
        public NestedDummyProperties nested;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static class NestedDummyProperties {

            public Set<Integer> ages;
        }
    }

    public static class SuperDummyProperties {
        @ConfigProperty(name = "lname")
        private String lastname;
        private NestedSuperDummyProperties supernested;

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public NestedSuperDummyProperties getSupernested() {
            return supernested;
        }

        public void setSupernested(NestedSuperDummyProperties supernested) {
            this.supernested = supernested;
        }

        public static class NestedSuperDummyProperties {

            @ConfigProperty(name = "afraid-of-heights")
            public Set<Integer> heights;
            @ConfigIgnore
            public Integer ignored;
        }
    }
}
