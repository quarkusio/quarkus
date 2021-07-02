package io.quarkus.registry.config.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.json.JsonArtifactCoordsMixin;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class RegistriesConfigMapperHelper {

    private static ObjectMapper yamlMapper;
    private static ObjectMapper jsonMapper;

    public static ObjectMapper yamlMapper() {
        return yamlMapper == null ? yamlMapper = initMapper(new ObjectMapper(new YAMLFactory())) : yamlMapper;
    }

    public static ObjectMapper jsonMapper() {
        return jsonMapper == null ? jsonMapper = initMapper(new ObjectMapper()) : jsonMapper;
    }

    private static ObjectMapper mapper(Path p) {
        return p.getFileName().toString().endsWith(".json") ? jsonMapper() : yamlMapper();
    }

    public static ObjectMapper initMapper(ObjectMapper mapper) {
        mapper.addMixIn(ArtifactCoords.class, JsonArtifactCoordsMixin.class);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static void serialize(Object config, Path p) throws IOException {
        if (!Files.exists(p.getParent())) {
            Files.createDirectories(p.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(p)) {
            mapper(p).writeValue(writer, config);
        }
    }

    public static void toJson(Object config, Writer writer) throws IOException {
        jsonMapper().writeValue(writer, config);
    }

    public static void toYaml(Object config, Writer writer) throws IOException {
        yamlMapper().writeValue(writer, config);
    }

    public static <T> T deserialize(Path p, Class<T> t) throws IOException {
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("File " + p + " does not exist");
        }
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            return mapper(p).readValue(reader, t);
        }
    }

    public static <T> T deserializeYaml(InputStream is, Class<T> t) throws IOException {
        return yamlMapper().readValue(is, t);
    }

    public static <T> T deserializeYaml(Reader reader, Class<T> t) throws IOException {
        return yamlMapper().readValue(reader, t);
    }
}
