package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class InterfaceWithOtherConfigPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class, Other.class)
                    .addAsResource(new StringAsset("dummy.other.name=test"), "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        Other other = dummyBean.dummyProperties.getOther();
        assertNotNull(other);
        assertEquals("test", other.getName());
    }

    @Singleton
    public static class DummyBean {

        @Inject
        public DummyProperties dummyProperties;
    }

    @ConfigProperties
    public interface DummyProperties {

        Other getOther();
    }

    public interface Other {

        String getName();
    }
}
