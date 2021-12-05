package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigPrefix;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleClassConfigPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.someNumbers=1,2,3,4\nother.name=redhat\nother.someNumbers=3,2,1"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        DummyProperties dummyProperties = dummyBean.dummyProperties;
        assertEquals("quarkus", dummyProperties.name);
        assertEquals("default", dummyProperties.unset);
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyProperties.someNumbers);

        DummyProperties dummyProperties2 = dummyBean.dummyProperties2;
        assertEquals("redhat", dummyProperties2.name);
        assertEquals("default", dummyProperties2.unset);
        assertEquals(Arrays.asList(3, 2, 1), dummyProperties2.someNumbers);
    }

    @Singleton
    public static class DummyBean {

        @Inject
        DummyProperties dummyProperties;

        @ConfigPrefix("other")
        DummyProperties dummyProperties2;

    }

    @ConfigProperties(prefix = "dummy", namingStrategy = ConfigProperties.NamingStrategy.VERBATIM)
    public static class DummyProperties {

        private String name;
        private String unset = "default";
        private List<Integer> someNumbers;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUnset() {
            return unset;
        }

        public void setUnset(String unset) {
            this.unset = unset;
        }

        public List<Integer> getSomeNumbers() {
            return someNumbers;
        }

        public void setSomeNumbers(List<Integer> someNumbers) {
            this.someNumbers = someNumbers;
        }
    }
}
