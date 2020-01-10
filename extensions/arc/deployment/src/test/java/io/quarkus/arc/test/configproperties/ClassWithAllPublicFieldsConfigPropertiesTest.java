package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassWithAllPublicFieldsConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.numbers=1,2,3,4\ndummy.bool-with-default=true\ndummy.optional-int=100"),
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
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties {

        public String name;
        public String unset = "default";
        public boolean boolWithDefault = false;
        public List<Integer> numbers;
        public Optional<Integer> optionalInt;
        public Optional<String> optionalString;
    }
}
