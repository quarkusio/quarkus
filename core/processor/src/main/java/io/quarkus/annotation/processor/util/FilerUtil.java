package io.quarkus.annotation.processor.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.bootstrap.util.PropertyUtils;

public class FilerUtil {

    private static final ObjectWriter JSON_OBJECT_WRITER;

    static {
        ObjectMapper jsonObjectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        JSON_OBJECT_WRITER = jsonObjectMapper.writerWithDefaultPrettyPrinter();
    }

    private final ProcessingEnvironment processingEnv;

    FilerUtil(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void write(String fileName, Set<String> set) {
        if (set.isEmpty()) {
            return;
        }

        try {
            final FileObject listResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    fileName);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(listResource.openOutputStream(), StandardCharsets.UTF_8))) {
                for (String className : set) {
                    writer.write(className);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + fileName + ": " + e);
            return;
        }
    }

    public void write(String fileName, Properties properties) {
        if (properties.isEmpty()) {
            return;
        }

        try {
            final FileObject propertiesResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    fileName);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(propertiesResource.openOutputStream(), StandardCharsets.UTF_8))) {
                PropertyUtils.store(properties, writer);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + fileName + ": " + e);
            return;
        }
    }

    public void writeJson(String fileName, Object value) {
        if (value == null) {
            return;
        }

        try {
            final FileObject jsonResource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    fileName);

            try (OutputStream os = jsonResource.openOutputStream()) {
                JSON_OBJECT_WRITER.writeValue(os, value);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write " + fileName + ": " + e);
            return;
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
}
