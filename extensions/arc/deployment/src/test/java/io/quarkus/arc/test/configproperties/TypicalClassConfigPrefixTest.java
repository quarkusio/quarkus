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

import io.quarkus.arc.config.ConfigPrefix;
import io.quarkus.test.QuarkusUnitTest;

public class TypicalClassConfigPrefixTest {

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
        DummyProperties dummyProperties = dummyBean.dummyProperties;
        assertEquals("quarkus", dummyProperties.getName());
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyProperties.getNumbers());
        assertEquals("default", dummyProperties.getUnset());
        assertTrue(dummyProperties.isBoolWithDefault());
        assertTrue(dummyProperties.getOptionalInt().isPresent());
        assertEquals(100, dummyProperties.getOptionalInt().get());
        assertFalse(dummyProperties.getOptionalString().isPresent());
    }

    @Singleton
    public static class DummyBean {
        @ConfigPrefix("dummy")
        DummyProperties dummyProperties;
    }

    public static class DummyProperties {

        private String name;
        private String unset = "default";
        private boolean boolWithDefault = false;
        private List<Integer> numbers;
        private Optional<Integer> optionalInt;
        private Optional<String> optionalString;

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

        public boolean isBoolWithDefault() {
            return boolWithDefault;
        }

        public void setBoolWithDefault(boolean boolWithDefault) {
            this.boolWithDefault = boolWithDefault;
        }

        public List<Integer> getNumbers() {
            return numbers;
        }

        public void setNumbers(List<Integer> numbers) {
            this.numbers = numbers;
        }

        public Optional<Integer> getOptionalInt() {
            return optionalInt;
        }

        public void setOptionalInt(Optional<Integer> optionalInt) {
            this.optionalInt = optionalInt;
        }

        public Optional<String> getOptionalString() {
            return optionalString;
        }

        public void setOptionalString(Optional<String> optionalString) {
            this.optionalString = optionalString;
        }
    }
}
