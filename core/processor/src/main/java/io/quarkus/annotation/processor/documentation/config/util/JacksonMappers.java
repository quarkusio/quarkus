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
    // Jackson 3 changed two defaults that break config model deserialization:
    // - FAIL_ON_NULL_FOR_PRIMITIVES now defaults to true; the writer uses NON_DEFAULT inclusion,
    //   so false booleans are omitted and read back as null.
    // - ALLOW_FINAL_FIELDS_AS_MUTATORS now defaults to false; the config model classes (e.g. ConfigRoot)
    //   use private final fields like items and qualifiedNames that are populated via reflection.
    private static final ObjectReader YAML_OBJECT_READER = YAMLMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
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
