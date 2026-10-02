package io.quarkus.gradle.application.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import io.quarkus.bootstrap.util.PropertyUtils;

public final class ResultReceiptProperties {

    private ResultReceiptProperties() {
    }

    public static void store(Map<String, String> properties, Path file) throws IOException {
        Properties target = new Properties();
        target.putAll(properties);
        StringWriter writer = new StringWriter();
        PropertyUtils.store(target, writer, null);
        Files.writeString(file, writer.toString().replace(System.lineSeparator(), "\n"));
    }
}
