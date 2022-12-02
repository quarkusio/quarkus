package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class IndexedPropertiesInjectionTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(IndexedBean.class)
                    .addAsServiceProvider(Converter.class, ConvertedValueConverter.class)
                    .addAsResource(new StringAsset(
                            "my.prop=1234\n" +
                                    "server.hosts[0]=localhost\n" +
                                    "server.hosts[1]=config\n" +
                                    "indexed.converted[0]=in\n" +
                                    "indexed.override.defaults[0]=e\n" +
                                    "indexed.override.defaults[1]=f\n" +
                                    "indexed.comma=a,b,c\n" +
                                    "indexed.comma[0]=a\n" +
                                    "indexed.comma[1]=b\n" +
                                    "optionals.indexed[0]=a\n" +
                                    "optionals.indexed[1]=b\n" +
                                    "supplier.indexed[0]=a\n" +
                                    "supplier.indexed[1]=b\n"),
                            "application.properties"));

    @Inject
    IndexedBean indexedBean;

    @Test
    void indexed() {
        assertEquals("localhost", indexedBean.getHost0());
        assertEquals("config", indexedBean.getHost1());
        assertEquals(Stream.of("localhost", "config").collect(Collectors.toList()), indexedBean.getHosts());
        assertEquals(Stream.of("localhost", "config").collect(Collectors.toSet()), indexedBean.getHostsSet());
        assertEquals(Stream.of(new ConvertedValue("out")).collect(Collectors.toList()), indexedBean.getConverted());
        assertEquals(Stream.of("a", "b", "c").collect(Collectors.toList()), indexedBean.getDefaults());
        assertEquals(Stream.of("e", "f").collect(Collectors.toList()), indexedBean.getOverrideDefaults());
        assertEquals(Stream.of("a", "b", "c").collect(Collectors.toList()), indexedBean.getComma());
    }

    @Test
    void optionals() {
        assertFalse(indexedBean.getOptionalEmpty().isPresent());
        assertTrue(indexedBean.getOptionalDefaults().isPresent());
        assertEquals(Stream.of("a", "b", "c").collect(Collectors.toList()), indexedBean.getOptionalDefaults().get());
        assertTrue(indexedBean.getOptionalIndexed().isPresent());
        assertEquals(Stream.of("a", "b").collect(Collectors.toList()), indexedBean.getOptionalIndexed().get());
    }

    @Test
    void suppliers() {
        assertThrows(NoSuchElementException.class, () -> indexedBean.getSupplierEmpty().get());
        assertEquals(Stream.of("a", "b", "c").collect(Collectors.toList()), indexedBean.getSupplierDefaults().get());
        assertEquals(Stream.of("a", "b").collect(Collectors.toList()), indexedBean.getSupplierIndexed().get());
    }

    @ApplicationScoped
    static class IndexedBean {
        @Inject
        @ConfigProperty(name = "server.hosts[0]")
        String host0;
        @Inject
        @ConfigProperty(name = "server.hosts[1]")
        String host1;
        @Inject
        @ConfigProperty(name = "server.hosts")
        List<String> hosts;
        @Inject
        @ConfigProperty(name = "server.hosts")
        Set<String> hostsSet;
        @Inject
        @ConfigProperty(name = "indexed.converted")
        List<ConvertedValue> converted;
        @Inject
        @ConfigProperty(name = "indexed.defaults", defaultValue = "a,b,c")
        List<String> defaults;
        @Inject
        @ConfigProperty(name = "indexed.override.defaults", defaultValue = "a,b,c")
        List<String> overrideDefaults;
        @Inject
        @ConfigProperty(name = "indexed.comma")
        List<String> comma;
        @Inject
        @ConfigProperty(name = "optionals.empty")
        Optional<List<String>> optionalEmpty;
        @Inject
        @ConfigProperty(name = "optionals.defaults", defaultValue = "a,b,c")
        Optional<List<String>> optionalDefaults;
        @Inject
        @ConfigProperty(name = "optionals.indexed")
        Optional<List<String>> optionalIndexed;
        @Inject
        @ConfigProperty(name = "supplier.empty")
        Supplier<List<String>> supplierEmpty;
        @Inject
        @ConfigProperty(name = "supplier.defaults", defaultValue = "a,b,c")
        Supplier<List<String>> supplierDefaults;
        @Inject
        @ConfigProperty(name = "supplier.indexed")
        Supplier<List<String>> supplierIndexed;

        public String getHost0() {
            return host0;
        }

        public String getHost1() {
            return host1;
        }

        public List<String> getHosts() {
            return hosts;
        }

        public Set<String> getHostsSet() {
            return hostsSet;
        }

        public List<ConvertedValue> getConverted() {
            return converted;
        }

        public List<String> getDefaults() {
            return defaults;
        }

        public List<String> getOverrideDefaults() {
            return overrideDefaults;
        }

        public List<String> getComma() {
            return comma;
        }

        public Optional<List<String>> getOptionalEmpty() {
            return optionalEmpty;
        }

        public Optional<List<String>> getOptionalDefaults() {
            return optionalDefaults;
        }

        public Optional<List<String>> getOptionalIndexed() {
            return optionalIndexed;
        }

        public Supplier<List<String>> getSupplierEmpty() {
            return supplierEmpty;
        }

        public Supplier<List<String>> getSupplierDefaults() {
            return supplierDefaults;
        }

        public Supplier<List<String>> getSupplierIndexed() {
            return supplierIndexed;
        }
    }

    public static class ConvertedValue {
        private final String value;

        public ConvertedValue(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ConvertedValue that = (ConvertedValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class ConvertedValueConverter implements Converter<ConvertedValue> {
        @Override
        public ConvertedValue convert(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new ConvertedValue("out");
        }
    }
}
