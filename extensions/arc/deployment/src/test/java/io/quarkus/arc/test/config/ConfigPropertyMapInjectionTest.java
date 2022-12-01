package io.quarkus.arc.test.config;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigPropertyMapInjectionTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsServiceProvider(Converter.class, VersionConverter.class)
                    .addAsResource(new StringAsset(
                            "root.numbers.1=one\n" +
                                    "root.numbers.2=two\n" +
                                    "root.numbers.3=three\n" +
                                    "versions.v1=1.The version 1.2.3\n" +
                                    "versions.v1.2=1.The version 1.2.0\n" +
                                    "versions.v2=2.The version 2.0.0\n"),
                            "application.properties"));

    @ConfigProperty(name = "root.numbers")
    Map<Integer, String> numbers;

    @ConfigProperty(name = "root.numbers")
    Optional<Map<Integer, String>> oNumbers;

    @ConfigProperty(name = "root.numbers")
    Supplier<Map<Integer, String>> sNumbers;

    @ConfigProperty(name = "versions")
    Map<String, Version> versions;

    @ConfigProperty(name = "default.versions", defaultValue = "v0.1=0.The version 0;v1\\=1\\;2\\;3=1.The version 1\\;2\\;3;v2\\=2\\;1\\;0=2.The version 2\\;1\\;0")
    Map<String, Version> versionsDefault;

    @Test
    void mapInjection() {
        assertNotNull(numbers);
        assertEquals(3, numbers.size());
        assertEquals("one", numbers.get(1));
        assertEquals("two", numbers.get(2));
        assertEquals("three", numbers.get(3));

        assertNotNull(numbers);
        assertEquals(3, numbers.size());
        assertEquals("one", numbers.get(1));
        assertEquals("two", numbers.get(2));
        assertEquals("three", numbers.get(3));

        assertNotNull(oNumbers);
        assertTrue(oNumbers.isPresent());
        assertEquals(3, oNumbers.get().size());
        assertEquals("one", oNumbers.get().get(1));
        assertEquals("two", oNumbers.get().get(2));
        assertEquals("three", oNumbers.get().get(3));

        assertNotNull(sNumbers);
        assertEquals(3, sNumbers.get().size());
        assertEquals("one", sNumbers.get().get(1));
        assertEquals("two", sNumbers.get().get(2));
        assertEquals("three", sNumbers.get().get(3));

        assertEquals(2, versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), versions.get("v2"));
        assertEquals(2, versionsDefault.size());
        assertEquals(new Version(1, "The version 1;2;3"), versionsDefault.get("v1=1;2;3"));
        assertEquals(new Version(2, "The version 2;1;0"), versionsDefault.get("v2=2;1;0"));
    }

    static class Version {
        int id;
        String name;

        Version(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Version version = (Version) o;
            return id == version.id && Objects.equals(name, version.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    public static class VersionConverter implements Converter<Version> {

        @Override
        public Version convert(String value) {
            return new Version(Integer.parseInt(value.substring(0, 1)), value.substring(2));
        }
    }
}
