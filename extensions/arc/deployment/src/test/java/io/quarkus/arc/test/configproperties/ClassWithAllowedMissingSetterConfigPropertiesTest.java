package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassWithAllowedMissingSetterConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.nested.numbers2=1,2\ndummy.unused=whatever"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.dummyProperties.name);
        assertEquals(Arrays.asList(1, 2), dummyBean.dummyProperties.nested.numbers2);
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;
    }

    @ConfigProperties(prefix = "dummy", failOnMismatchingMember = false)
    public static class DummyProperties {

        private String name;
        private List<Integer> numbers;
        private NestedDummyProperties nested;

        public void setName(String name) {
            this.name = name;
        }

        public void setNested(NestedDummyProperties nested) {
            this.nested = nested;
        }
    }

    public static class NestedDummyProperties extends ParentOfNestedDummyProperties {
        private String name2;
        private List<Integer> numbers2;

        public void setNumbers2(List<Integer> numbers2) {
            this.numbers2 = numbers2;
        }
    }

    public static class ParentOfNestedDummyProperties {
        private String whatever;
    }
}
