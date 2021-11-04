package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigPrefix;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleInterfaceConfigPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.numbers=1,2,3,4\nother.name=redhat\nother.numbers=3,2,1"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals("quarkus!", dummyBean.nameWithSuffix());
        assertEquals(Arrays.asList(1, 2, 3, 4), dummyBean.getNumbers());

        assertEquals("redhat", dummyBean.getName2());
        assertEquals("redhat!", dummyBean.nameWithSuffix2());
        assertEquals(Arrays.asList(3, 2, 1), dummyBean.getNumbers2());
    }

    @Singleton
    public static class DummyBean {

        @Inject
        DummyProperties dummyProperties;

        @ConfigPrefix("other")
        DummyProperties otherProperties;

        String getName() {
            return dummyProperties.getFirstName();
        }

        Collection<Integer> getNumbers() {
            return dummyProperties.getNumbers();
        }

        String nameWithSuffix() {
            return dummyProperties.nameWithSuffix();
        }

        String getName2() {
            return otherProperties.getFirstName();
        }

        Collection<Integer> getNumbers2() {
            return otherProperties.getNumbers();
        }

        String nameWithSuffix2() {
            return otherProperties.nameWithSuffix();
        }
    }

    @ConfigProperties(prefix = "dummy")
    public interface DummyProperties {

        @ConfigProperty(name = "name")
        String getFirstName();

        Collection<Integer> getNumbers();

        default String nameWithSuffix() {
            return getFirstName() + "!";
        }

        static void someStatic() {

        }
    }
}
