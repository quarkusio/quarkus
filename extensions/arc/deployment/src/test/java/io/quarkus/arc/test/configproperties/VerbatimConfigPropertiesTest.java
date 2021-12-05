package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class VerbatimConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "dummy.fooBar=quarkus1\ndummy.foo=quarkus2\ndummy.unused=whatever"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus1", dummyBean.getFooBar());
        assertEquals("quarkus2", dummyBean.getFoo());
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;

        String getFoo() {
            return dummyProperties.getFoo();
        }

        String getFooBar() {
            return dummyProperties.getFooBar();
        }
    }

    @ConfigProperties(prefix = "dummy", namingStrategy = ConfigProperties.NamingStrategy.VERBATIM)
    public static class DummyProperties {

        public String foo;
        public String fooBar;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getFooBar() {
            return fooBar;
        }

        public void setFooBar(String fooBar) {
            this.fooBar = fooBar;
        }
    }
}
