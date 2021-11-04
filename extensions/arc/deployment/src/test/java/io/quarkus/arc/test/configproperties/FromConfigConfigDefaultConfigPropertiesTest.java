package io.quarkus.arc.test.configproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class FromConfigConfigDefaultConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, DummyProperties.class)
                    .addAsResource(new StringAsset(
                            "quarkus.arc.config-properties-default-naming-strategy=verbatim\ndummy.fooBarDev=quarkus1\ndummy2.foo-bar=quarkus2"),
                            "application.properties"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        assertEquals("quarkus2", dummyBean.getFooBar());
        assertEquals("quarkus1", dummyBean.getFooBarDev());
    }

    @Singleton
    public static class DummyBean {
        @Inject
        DummyProperties dummyProperties;

        @Inject
        DummyProperties2 dummyProperties2;

        String getFooBar() {
            return dummyProperties2.getFooBar();
        }

        String getFooBarDev() {
            return dummyProperties.getFooBarDev();
        }
    }

    @ConfigProperties(prefix = "dummy")
    public static class DummyProperties {

        public String fooBarDev;

        public String getFooBarDev() {
            return fooBarDev;
        }

        public void setFooBarDev(String fooBarDev) {
            this.fooBarDev = fooBarDev;
        }
    }

    @ConfigProperties(prefix = "dummy2", namingStrategy = ConfigProperties.NamingStrategy.KEBAB_CASE)
    public static class DummyProperties2 {

        public String fooBar;

        public String getFooBar() {
            return fooBar;
        }

        public void setFooBar(String fooBar) {
            this.fooBar = fooBar;
        }
    }
}
