package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigPrefix;
import io.quarkus.test.QuarkusUnitTest;

public class TypicalInterfaceConfigPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.numbers=1,2,3,4\ndummy.boolWD=true\ndummy.optional-int=100\ndummy.optionalStringList=a,b"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals("quarkus!", dummyBean.nameWithSuffix());
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyBean.getNumbers());
        assertTrue(dummyBean.isBoolWithDefault());
        assertEquals(1.0, dummyBean.getDoubleWithDefault());
        assertTrue(dummyBean.getOptionalInt().isPresent());
        assertEquals(100, dummyBean.getOptionalInt().get());
        assertFalse(dummyBean.getOptionalString().isPresent());
        assertFalse(dummyBean.getIntListOptional().isPresent());
        assertTrue(dummyBean.getStringListOptional().isPresent());
        assertEquals(Arrays.asList("a", "b"), dummyBean.getStringListOptional().get());
    }

    @Singleton
    public static class DummyBean {

        @ConfigPrefix("dummy")
        DummyProperties dummyProperties;

        String getName() {
            return dummyProperties.getFirstName();
        }

        Collection<Integer> getNumbers() {
            return dummyProperties.getNumbers();
        }

        boolean isBoolWithDefault() {
            return dummyProperties.isBoolWithDef();
        }

        Double getDoubleWithDefault() {
            return dummyProperties.doubleWithDefault();
        }

        Optional<Integer> getOptionalInt() {
            return dummyProperties.getOptionalInt();
        }

        Optional<String> getOptionalString() {
            return dummyProperties.stringOptional();
        }

        Optional<List<String>> getStringListOptional() {
            return dummyProperties.stringListOptional();
        }

        Optional<List<Integer>> getIntListOptional() {
            return dummyProperties.intListOptional();
        }

        String nameWithSuffix() {
            return dummyProperties.nameWithSuffix();
        }
    }

    public interface DummyProperties {

        @ConfigProperty(name = "name")
        String getFirstName();

        @ConfigProperty(name = "boolWD", defaultValue = "false")
        boolean isBoolWithDef();

        @ConfigProperty(name = "doubleWithDefault", defaultValue = "1.0")
        Double doubleWithDefault();

        Collection<Integer> getNumbers();

        Optional<Integer> getOptionalInt();

        @ConfigProperty(name = "optionalString")
        Optional<String> stringOptional();

        @ConfigProperty(name = "optionalStringList")
        Optional<List<String>> stringListOptional();

        @ConfigProperty(name = "optionalIntList")
        Optional<List<Integer>> intListOptional();

        default String nameWithSuffix() {
            return getFirstName() + "!";
        }
    }
}
