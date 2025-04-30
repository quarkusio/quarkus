package io.quarkus.config;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;

class MapConverterTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("map.value", "key1:value1,key2:value2");

    @Inject
    MapMapping mapping;

    @Test
    void mapConverter() {
        Map<String, String> value = mapping.value();
        System.out.println(value);
    }

    @ConfigMapping(prefix = "map")
    interface MapMapping {
        @WithConverter(MapConverter.class)
        Map<String, String> value();
    }

    public static class MapConverter implements Converter<Map<String, String>> {
        @Override
        public Map<String, String> convert(final String value) throws IllegalArgumentException, NullPointerException {
            return Arrays.stream(value.split(","))
                    .map(entry -> entry.split(":"))
                    .collect(Collectors.toMap(entry -> entry[0].trim(), entry -> entry[1].trim()));
        }
    }
}
