package io.quarkus.annotation.processor.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.bootstrap.util.PropertyUtils;

public class FilerUtil {

    private static final ObjectWriter JSON_OBJECT_WRITER;
    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    static {
    }

    static {
        ObjectMapper jsonObjectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        JSON_OBJECT_WRITER = jsonObjectMapper.writerWithDefaultPrettyPrinter();

        YAML_OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
    }

    private final ProcessingEnvironment processingEnv;

    FilerUtil(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void write(Path filePath, Set<String> set) {
        if (set.isEmpty()) {
            return;
        }

        try {
            final FileObject listResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath.toString());

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(listResource.openOutputStream(), StandardCharsets.UTF_8))) {
                for (String className : set) {
                    writer.write(className);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + filePath + ": " + e);
            return;
        }
    }

    public void write(Path filePath, Properties properties) {
        if (properties.isEmpty()) {
            return;
        }

        try {
            final FileObject propertiesResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath.toString());

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(propertiesResource.openOutputStream(), StandardCharsets.UTF_8))) {
                PropertyUtils.store(properties, writer);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + filePath + ": " + e);
            return;
        }
    }

    public void writeJson(Path filePath, Object value) {
        if (value == null) {
            return;
        }

        try {
            final FileObject jsonResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath.toString());

            try (OutputStream os = jsonResource.openOutputStream()) {
                JSON_OBJECT_WRITER.writeValue(os, value);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + filePath + ": " + e);
            return;
        }
    }

    /**
     * The model files are written outside of target/classes as we don't want to include them in the jar.
     */
    public Path writeModel(Path filePath, Object value) {
        Path yamlModelPath = getTargetPath().resolve(filePath);
        try {
            Files.createDirectories(yamlModelPath.getParent());
            YAML_OBJECT_MAPPER.writeValue(yamlModelPath.toFile(), value);

            return yamlModelPath;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write the model to: " + yamlModelPath, e);
        }
    }

    public Path getTargetPath() {
        try {
            FileObject dummyFile = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "dummy");
            return Paths.get(dummyFile.toUri()).getParent().getParent();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to determine the path of target/" + e);
            throw new UncheckedIOException(e);
        }
    }

    public Optional<Path> getPomPath() {
        try {
            Path pomPath = Paths.get(processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "dummy").toUri())
                    .getParent().getParent().getParent().resolve("pom.xml");

            if (!Files.isReadable(pomPath)) {
                return Optional.empty();
            }

            return Optional.of(pomPath.toAbsolutePath());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> getExtensionMetadata() {
        String extensionMetadataDescriptor = "META-INF/quarkus-extension.yaml";

        try {
            FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "",
                    extensionMetadataDescriptor);
            if (fileObject == null) {
                return Optional.empty();
            }

            try (InputStream is = fileObject.openInputStream()) {
                String yamlMetadata = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> extensionMetadata = YAML_OBJECT_MAPPER.readValue(yamlMetadata, Map.class);

                return Optional.of(extensionMetadata);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.WARNING,
                    "Unable to read extension metadata file: " + extensionMetadataDescriptor + " because of " + e.getMessage());
            return Optional.empty();
        }
    }
}
