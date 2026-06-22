package io.quarkus.registry.catalog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TimeZone;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonArtifactCoordsMixin;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class CatalogMapperHelper {

    private static ObjectMapper mapper;
    private static ObjectMapper yamlMapper;

    public static ObjectMapper mapper() {
        return mapper == null ? mapper = initMapper(JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)) : mapper;
    }

    private static ObjectMapper yamlMapper() {
        return yamlMapper == null ? yamlMapper = initMapper(YAMLMapper.builder()) : yamlMapper;
    }

    private static ObjectMapper mapperForPath(Path p) {
        return p.getFileName().toString().endsWith("json") ? mapper() : yamlMapper();
    }

    public static ObjectMapper initMapper(MapperBuilder<?, ?> builder) {
        builder.addMixIn(ArtifactCoords.class, JsonArtifactCoordsMixin.class);
        builder.enable(SerializationFeature.INDENT_OUTPUT);
        builder.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        builder.propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        builder.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        builder.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        builder.defaultLocale(Locale.US);
        builder.defaultTimeZone(TimeZone.getTimeZone("UTC"));
        return builder.build();
    }

    public static void serialize(Object catalog, Path p) throws IOException {
        serialize(mapperForPath(p), catalog, p);
    }

    public static void serialize(ObjectMapper mapper, Object catalog, Path p) throws IOException {
        final Path parent = p.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(p)) {
            serialize(mapper, catalog, writer);
        }
    }

    public static void serialize(Object catalog, Writer writer) throws IOException {
        serialize(mapper(), catalog, writer);
    }

    public static void serialize(ObjectMapper mapper, Object catalog, Writer writer) throws IOException {
        mapper.writeValue(writer, catalog);
    }

    public static <T> T deserialize(Path p, Class<T> t) throws IOException {
        return deserialize(mapperForPath(p), p, t);
    }

    public static <T> T deserialize(ObjectMapper mapper, Path p, Class<T> t) throws IOException {
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("File " + p + " does not exist");
        }
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            return mapper.readValue(reader, t);
        }
    }

    public static <T> T deserialize(InputStream is, Class<T> t) throws IOException {
        return deserialize(mapper(), is, t);
    }

    public static <T> T deserialize(ObjectMapper mapper, InputStream is, Class<T> t) throws IOException {
        return mapper.readValue(is, t);
    }

    public static <T> T deserialize(Reader reader, Class<T> t) throws IOException {
        return deserialize(mapper(), reader, t);
    }

    public static <T> T deserialize(ObjectMapper mapper, Reader reader, Class<T> t) throws IOException {
        return mapper.readValue(reader, t);
    }
}
