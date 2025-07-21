package io.quarkus.annotation.processor.documentation.config.util;

import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public final class JacksonMappers {

    private static final ObjectWriter JSON_OBJECT_WRITER = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .setLocale(Locale.US)
            .setTimeZone(TimeZone.getTimeZone("UTC"))
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT).writerWithDefaultPrettyPrinter();
    private static final ObjectWriter YAML_OBJECT_WRITER = new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT).writer();
    private static final ObjectReader YAML_OBJECT_READER = new ObjectMapper(new YAMLFactory())
            .registerModule(new ParameterNamesModule()).reader();

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
