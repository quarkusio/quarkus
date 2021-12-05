package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassWithAllPublicFieldsAndSuperclassConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class, SuperDummyProperties.class,
                            SuperDuperDummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.numbers=1,2,3,4\ndummy.bool-with-default=true\ndummy.optional-int=100\ndummy.optional-int-list=1,2"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyBean.getNumbers());
        assertEquals("default", dummyBean.getUnset());
        assertTrue(dummyBean.isBoolWithDefault());
        assertTrue(dummyBean.getOptionalInt().isPresent());
        assertEquals(100, dummyBean.getOptionalInt().get());
        assertFalse(dummyBean.getOptionalString().isPresent());
        assertFalse(dummyBean.getOptionalStringList().isPresent());
        assertTrue(dummyBean.getOptionalIntList().isPresent());
        assertEquals(Arrays.asList(1, 2), dummyBean.getOptionalIntList().get());

        assertEquals("s", DummyProperties.SOME_STRING);
        assertEquals(1, DummyProperties.SOME_INT);
        assertEquals("s2", SuperDuperDummyProperties.SOME_OTHER_STRING);
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;

        String getName() {
            return dummyProperties.name;
        }

        List<Integer> getNumbers() {
            return dummyProperties.numbers;
        }

        String getUnset() {
            return dummyProperties.unset;
        }

        boolean isBoolWithDefault() {
            return dummyProperties.boolWithDefault;
        }

        Optional<Integer> getOptionalInt() {
            return dummyProperties.optionalInt;
        }

        Optional<String> getOptionalString() {
            return dummyProperties.optionalString;
        }

        Optional<List<String>> getOptionalStringList() {
            return dummyProperties.optionalStringList;
        }

        Optional<List<Integer>> getOptionalIntList() {
            return dummyProperties.optionalIntList;
        }
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties extends SuperDummyProperties {

        private static final String SOME_STRING = "s";
        public static final int SOME_INT = 1;

        public String name;
        public String unset = "default";
        public List<Integer> numbers;
        public Optional<Integer> optionalInt;
        public Optional<List<String>> optionalStringList;
    }

    public static class SuperDummyProperties extends SuperDuperDummyProperties {

        public boolean boolWithDefault = false;
        public Optional<String> optionalString;
    }

    public static class SuperDuperDummyProperties {

        static final String SOME_OTHER_STRING = "s2";

        public Optional<List<Integer>> optionalIntList;
    }
}
