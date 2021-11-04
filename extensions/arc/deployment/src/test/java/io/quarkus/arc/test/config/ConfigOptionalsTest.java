package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigOptionalsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UsingOptionals.class)
                    .addAsResource(new StringAsset("foo=42\nbar=4.2"), "application.properties"));

    @Inject
    UsingOptionals usingOptionals;

    @Test
    public void testOptionals() {
        assertTrue(usingOptionals.optionalOfInteger.isPresent());
        assertEquals(42, usingOptionals.optionalOfInteger.get());

        assertTrue(usingOptionals.optionalOfLong.isPresent());
        assertEquals(42, usingOptionals.optionalOfLong.get());

        assertTrue(usingOptionals.optionalOfDouble.isPresent());
        assertEquals(4.2, usingOptionals.optionalOfDouble.get());

        assertTrue(usingOptionals.optionalInt.isPresent());
        assertEquals(42, usingOptionals.optionalInt.getAsInt());

        assertTrue(usingOptionals.optionalLong.isPresent());
        assertEquals(42, usingOptionals.optionalLong.getAsLong());

        assertTrue(usingOptionals.optionalDouble.isPresent());
        assertEquals(4.2, usingOptionals.optionalDouble.getAsDouble());

        assertFalse(usingOptionals.missingOptionalOfInteger.isPresent());
        assertFalse(usingOptionals.missingOptionalOfLong.isPresent());
        assertFalse(usingOptionals.missingOptionalOfDouble.isPresent());
        assertFalse(usingOptionals.missingOptionalInt.isPresent());
        assertFalse(usingOptionals.missingOptionalLong.isPresent());
        assertFalse(usingOptionals.missingOptionalDouble.isPresent());
    }

    @Singleton
    static class UsingOptionals {
        @Inject
        @ConfigProperty(name = "foo")
        Optional<Integer> optionalOfInteger;

        @Inject
        @ConfigProperty(name = "foo")
        Optional<Long> optionalOfLong;

        @Inject
        @ConfigProperty(name = "bar")
        Optional<Double> optionalOfDouble;

        @Inject
        @ConfigProperty(name = "foo")
        OptionalInt optionalInt;

        @Inject
        @ConfigProperty(name = "foo")
        OptionalLong optionalLong;

        @Inject
        @ConfigProperty(name = "bar")
        OptionalDouble optionalDouble;

        @Inject
        @ConfigProperty(name = "missing")
        Optional<Integer> missingOptionalOfInteger;

        @Inject
        @ConfigProperty(name = "missing")
        Optional<Long> missingOptionalOfLong;

        @Inject
        @ConfigProperty(name = "missing")
        Optional<Double> missingOptionalOfDouble;

        @Inject
        @ConfigProperty(name = "missing")
        OptionalInt missingOptionalInt;

        @Inject
        @ConfigProperty(name = "missing")
        OptionalLong missingOptionalLong;

        @Inject
        @ConfigProperty(name = "missing")
        OptionalDouble missingOptionalDouble;
    }

}
