package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class VerbatimInterfaceConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.bool-with-default=true\ndummy.optional-int=100\ndummy.numbers=1,2,3,4"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyBean.numbers());
        assertTrue(dummyBean.boolWithDefault());
        assertTrue(dummyBean.getOptionalInt().isPresent());
        assertEquals(100, dummyBean.getOptionalInt().get());
    }

    @Singleton
    public static class DummyBean {

        @Inject
        DummyProperties dummyProperties;

        Collection<Integer> numbers() {
            return dummyProperties.numbersWithoutDefault();
        }

        boolean boolWithDefault() {
            return dummyProperties.boolWithDefault();
        }

        Optional<Integer> getOptionalInt() {
            return dummyProperties.optionalInt();
        }
    }

    @ConfigProperties(prefix = "dummy", namingStrategy = ConfigProperties.NamingStrategy.KEBAB_CASE)
    public interface DummyProperties {

        @ConfigProperty(defaultValue = "false")
        boolean boolWithDefault();

        @ConfigProperty(name = "numbers")
        Collection<Integer> numbersWithoutDefault();

        Optional<Integer> optionalInt();
    }
}
