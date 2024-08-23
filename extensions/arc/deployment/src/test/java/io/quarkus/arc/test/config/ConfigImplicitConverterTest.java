package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigImplicitConverterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Configured.class, Foo.class, Bar.class)
                    .addAsResource(new StringAsset("foo=1\nbar=1"), "application.properties"));

    @Inject
    Configured configured;

    @Test
    public void testFoo() {
        assertEquals("1", configured.getFooValue());
        assertEquals("1", configured.getBarProviderValue());
    }

    @ApplicationScoped
    static class Configured {

        @Inject
        @ConfigProperty(name = "foo")
        Foo foo;

        @ConfigProperty(name = "bar")
        Provider<Bar> barProvider;

        String getFooValue() {
            return foo != null ? foo.value : null;
        }

        String getBarProviderValue() {
            return barProvider.get().value;
        }

    }

    public static class Foo {

        String value;

        public Foo(String value) {
            this.value = value;
        }

    }

    public static class Bar {

        String value;

        public Bar(String value) {
            this.value = value;
        }

    }

}
