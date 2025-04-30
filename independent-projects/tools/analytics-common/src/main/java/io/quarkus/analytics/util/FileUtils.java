package io.quarkus.analytics.util;

import static io.quarkus.analytics.util.StringUtils.getObjectMapper;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.devtools.messagewriter.MessageWriter;

public class FileUtils {

    /**
     * Creates the file for the given path and the folder that contains it.
     * Does nothing if it any of those already exist.
     *
     * @param path the file to create
     *
     * @throws IOException if the file operation fails
     */
    public static void createFileAndParent(Path path) throws IOException {
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    /**
     * Writes a String to file
     *
     * @param content
     * @param path
     * @throws IOException
     */
    public static void append(String content, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.append(content);
        }
    }

    /**
     * Writes an object, as JSON to file
     *
     * @param content
     * @param path
     * @param <T>
     * @throws IOException
     */
    public static <T> void write(T content, Path path) throws IOException {
        final ObjectMapper mapper = getObjectMapper();
        mapper.writeValue(path.toFile(), content);
    }

    /**
     * Writes an object, as JSON to file. Deletes previous file if it exists, before writing the new one.
     *
     * @param content
     * @param path
     * @param <T>
     * @throws IOException
     */
    public static <T> void overwrite(T content, Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
        createFileAndParent(path);
        final ObjectMapper mapper = getObjectMapper();
        mapper.writeValue(path.toFile(), content);
    }

    public static <T> Optional<T> read(Class<T> clazz, Path path, MessageWriter log) throws IOException {
        try {
            final ObjectMapper mapper = getObjectMapper();
            return Optional.of(mapper.readValue(path.toFile(), clazz));
        } catch (Exception e) {
            log.warn("[Quarkus build analytics] Could not read {}", path.toString(), e);
            return Optional.empty();
        } catch (Throwable t) {
            log.error("[Quarkus build analytics] Unexpected error reading class " + t.getClass().getName() +
                    " from path: " + path.toString() +
                    ". Got message: " + t.getMessage() +
                    ". Attempting to continue...");
            return Optional.empty();
        }
    }
}
