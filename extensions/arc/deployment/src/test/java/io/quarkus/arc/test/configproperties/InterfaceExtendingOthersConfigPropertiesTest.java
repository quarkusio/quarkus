package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class InterfaceExtendingOthersConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.numbers=1.0,2.0\ndummy.bool=true"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals(Arrays.asList(1.0, 2.0), dummyBean.getNumbers());
        assertTrue(dummyBean.isBool());
    }

    @Singleton
    public static class DummyBean {

        @Inject
        DummyProperties dummyProperties;

        String getName() {
            return dummyProperties.getFirstName();
        }

        List<Double> getNumbers() {
            return dummyProperties.numbers();
        }

        boolean isBool() {
            return dummyProperties.getBool();
        }
    }

    @ConfigProperties(prefix = "dummy")
    public interface DummyProperties extends NumbersProvider, BoolSupplier {

        @ConfigProperty(name = "name")
        String getFirstName();
    }

    public interface NumbersProvider {

        @ConfigProperty(name = "numbers")
        List<Double> numbers();
    }

    public interface BoolSupplier {

        boolean getBool();
    }
}
