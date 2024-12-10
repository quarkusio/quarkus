package io.quarkus.dockerfiles.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.qute.Qute;
import io.quarkus.qute.Qute.Fmt;

public class DockerfileContent {

    public static final String FROM = "from";
    public static final String TYPE = "type";
    public static final String APPLICATION_NAME = "application-name";
    public static final String OUTPUT_DIR = "output-dir";

    public static String getJvmDockerfileContent(Map<String, String> data) {
        Fmt fmt = Qute.fmt(readResource("Dockerfile.tpl.qute.jvm"));
        for (Map.Entry<String, String> entry : data.entrySet()) {
            fmt = fmt.data(entry.getKey(), entry.getValue());
        }
        return fmt.render();
    }

    public static String getJvmDockerfileContent(String from, String name, Path outputDir) {
        return getJvmDockerfileContent(
                Map.of(FROM, from, TYPE, "jvm", APPLICATION_NAME, name, OUTPUT_DIR, outputDir.toString()));
    }

    public static String getNativeDockerfileContent(Map<String, String> data) {
        Fmt fmt = Qute.fmt(readResource("Dockerfile.tpl.qute.native"));
        for (Map.Entry<String, String> entry : data.entrySet()) {
            fmt = fmt.data(entry.getKey(), entry.getValue());
        }
        return fmt.render();
    }

    public static String getNativeDockerfileContent(String from, String name, Path outputDir) {
        return getNativeDockerfileContent(
                Map.of(FROM, from, TYPE, "native", APPLICATION_NAME, name, OUTPUT_DIR, outputDir.toString()));
    }

    private static String readResource(String resource) {
        try (InputStream in = DockerfileContent.class.getClassLoader().getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), Charset.defaultCharset());
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}
