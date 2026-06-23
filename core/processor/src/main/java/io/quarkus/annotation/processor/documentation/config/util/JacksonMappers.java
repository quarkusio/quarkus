package io.quarkus.annotation.processor.documentation.config.util;

import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public final class JacksonMappers {

    private static final ObjectWriter JSON_OBJECT_WRITER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .defaultLocale(Locale.US)
            .defaultTimeZone(TimeZone.getTimeZone("UTC"))
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
            .build().writerWithDefaultPrettyPrinter();
    private static final ObjectWriter YAML_OBJECT_WRITER = YAMLMapper.builder()
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
            .build().writer();
    // Jackson 2 coerced null to false for primitives; Jackson 3 rejects it by default.
    // The writer uses NON_DEFAULT inclusion, so false booleans are omitted and read back as null.
    private static final ObjectReader YAML_OBJECT_READER = YAMLMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build().reader();

    private JacksonMappers() {
    }

    public static ObjectWriter jsonObjectWriter() {
        return JSON_OBJECT_WRITER;
    }

    public static ObjectWriter yamlObjectWriter() {
        return YAML_OBJECT_WRITER;
    }

    public static ObjectReader yamlObjectReader() {
        return YAML_OBJECT_READER;
    }
}
