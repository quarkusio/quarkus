package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class NestedClassAndSuperclassConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBean.class, DummyProperties.class, DummyProperties.NestedDummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.lastname=redhat\ndummy.name=quarkus\ndummy.nested.ages=1,2,3,4\ndummy.supernested.heights=100,200\ndummy.unused=whatever\ndummy.nested.unused=whatever2"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals("redhat", dummyBean.getLastname());
        assertEquals(4, dummyBean.getAge().size());
        assertTrue(dummyBean.getAge().contains(1));
        assertTrue(dummyBean.getAge().contains(2));
        assertTrue(dummyBean.getAge().contains(3));
        assertTrue(dummyBean.getAge().contains(4));
        assertEquals(2, dummyBean.getHeights().size());
        assertTrue(dummyBean.getHeights().contains(100));
        assertTrue(dummyBean.getHeights().contains(200));
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;

        String getName() {
            return dummyProperties.getName();
        }

        String getLastname() {
            return dummyProperties.getLastname();
        }

        Set<Integer> getAge() {
            return dummyProperties.nested.ages;
        }

        Set<Integer> getHeights() {
            return dummyProperties.getSupernested().heights;
        }
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

            public Set<Integer> heights;
        }
    }
}
