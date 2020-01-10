package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class TypicalInterfaceConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.numbers=1,2,3,4\ndummy.boolWD=true\ndummy.optional-int=100"),
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
    }

    @Singleton
    public static class DummyBean {

        @Inject
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

        String nameWithSuffix() {
            return dummyProperties.nameWithSuffix();
        }
    }

    @ConfigProperties(prefix = "dummy")
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

        default String nameWithSuffix() {
            return getFirstName() + "!";
        }
    }
}
