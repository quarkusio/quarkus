package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class InterfaceWithNestedInterfaceConfigPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class, DummyProperties.NestedDummy.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ndummy.boolWD=true\ndummy.nested-dummy.foo=bar\ndummy.nested-dummy.other=some\ndummy.other-nested.foo=bar2\ndummy.other-nested.other=some2"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.dummyProperties.getName());
        assertTrue(dummyBean.dummyProperties.isBoolWithDef());

        DummyProperties.NestedDummy nestedDummy = dummyBean.dummyProperties.getNestedDummy();
        assertNotNull(nestedDummy);
        assertEquals("some", nestedDummy.getOther());
        assertEquals("bar", nestedDummy.getFooBar());
        assertEquals(Arrays.asList(1, 2, 3), nestedDummy.getNumbers());

        DummyProperties.NestedDummy otherNested = dummyBean.dummyProperties.getOtherNested();
        assertNotNull(otherNested);
        assertEquals("some2", otherNested.getOther());
        assertEquals("bar2", otherNested.getFooBar());
        assertEquals(Arrays.asList(1, 2, 3), otherNested.getNumbers());
    }

    @Singleton
    public static class DummyBean {

        @Inject
        public DummyProperties dummyProperties;
    }

    @ConfigProperties(prefix = "dummy")
    public interface DummyProperties {

        String getName();

        @ConfigProperty(name = "boolWD", defaultValue = "false")
        boolean isBoolWithDef();

        NestedDummy getNestedDummy();

        NestedDummy getOtherNested();

        interface NestedDummy {

            @ConfigProperty(defaultValue = "1,2,3")
            Collection<Integer> getNumbers();

            @ConfigProperty(name = "foo")
            String getFooBar();

            String getOther();
        }
    }
}
