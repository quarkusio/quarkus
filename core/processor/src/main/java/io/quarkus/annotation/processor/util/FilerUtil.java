package io.quarkus.annotation.processor.util;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

import io.quarkus.annotation.processor.documentation.config.util.JacksonMappers;
import io.quarkus.bootstrap.util.PropertyUtils;

public class FilerUtil {

    private final ProcessingEnvironment processingEnv;

    FilerUtil(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * This method uses the annotation processor Filer API and we shouldn't use a Path as paths containing \ are not supported.
     */
    public void write(String filePath, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            final FileObject listResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(listResource.openOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(value);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + filePath + ": " + e);
            return;
        }
    }

    /**
     * This method uses the annotation processor Filer API and we shouldn't use a Path as paths containing \ are not supported.
     */
    public void write(String filePath, Set<String> set) {
        if (set.isEmpty()) {
            return;
        }

        try {
            final FileObject listResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath);

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

    /**
     * This method uses the annotation processor Filer API and we shouldn't use a Path as paths containing \ are not supported.
     */
    public void write(String filePath, Properties properties) {
        if (properties.isEmpty()) {
            return;
        }

        try {
            final FileObject propertiesResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(propertiesResource.openOutputStream(), StandardCharsets.UTF_8))) {
                PropertyUtils.store(properties, writer);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + filePath + ": " + e);
            return;
        }
    }

    /**
     * This method uses the annotation processor Filer API and we shouldn't use a Path as paths containing \ are not supported.
     */
    public void writeJson(String filePath, Object value) {
        if (value == null) {
            return;
        }

        try {
            final FileObject jsonResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath);

            try (OutputStream os = jsonResource.openOutputStream()) {
                JacksonMappers.jsonObjectWriter().writeValue(os, value);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + filePath + ": " + e);
            return;
        }
    }

    /**
     * This method uses the annotation processor Filer API and we shouldn't use a Path as paths containing \ are not supported.
     */
    public void writeYaml(String filePath, Object value) {
        if (value == null) {
            return;
        }

        try {
            final FileObject yamlResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    filePath);

            try (OutputStream os = yamlResource.openOutputStream()) {
                JacksonMappers.yamlObjectWriter().writeValue(os, value);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + filePath + ": " + e);
            return;
        }
    }

    /**
     * The model files are written outside of target/classes as we don't want to include them in the jar.
     * <p>
     * They are not written by the annotation processor Filer API so we can use proper Paths.
     */
    public Path writeModel(String filePath, Object value) {
        Path yamlModelPath = getTargetPath().resolve(filePath);
        try {
            Files.createDirectories(yamlModelPath.getParent());
            JacksonMappers.yamlObjectWriter().writeValue(yamlModelPath.toFile(), value);

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
                Map<String, Object> extensionMetadata = JacksonMappers.yamlObjectReader().readValue(yamlMetadata, Map.class);

                return Optional.of(extensionMetadata);
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            // ignore
            // we could get the URI, create a Path and check that the path exists but it seems a bit overkill
            return Optional.empty();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.WARNING,
                    "Unable to read extension metadata file: " + extensionMetadataDescriptor + " because of "
                            + e.getClass().getName() + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
