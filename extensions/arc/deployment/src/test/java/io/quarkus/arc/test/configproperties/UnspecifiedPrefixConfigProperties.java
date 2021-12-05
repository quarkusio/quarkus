package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class UnspecifiedPrefixConfigProperties {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class, GreetingExtraProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.name=quarkus\ngreeting-extra.message=hello"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean; // used to prevent bean from being removed since it's not used anywhere else

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus", dummyBean.getName());
        assertEquals("hello", dummyBean.getMessage());
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;

        @Inject
        GreetingExtraProperties greetingExtraProperties;

        String getName() {
            return dummyProperties.name;
        }

        String getMessage() {
            return greetingExtraProperties.getMessage();
        }
    }

    @ConfigProperties
    public static class DummyProperties {

        public String name;
    }

}
