package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigImplicitConverterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Configured.class)
                    .addAsResource(new StringAsset("foo=1"), "application.properties"));

    @Inject
    Configured configured;

    @Test
    public void testFoo() {
        assertEquals("1", configured.getFooValue());
    }

    @ApplicationScoped
    static class Configured {

        @Inject
        @ConfigProperty(name = "foo")
        Foo foo;

        String getFooValue() {
            return foo != null ? foo.value : null;
        }
    }

    public static class Foo {

        String value;

        public Foo(String value) {
            this.value = value;
        }

    }

}
