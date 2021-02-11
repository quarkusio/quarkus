package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A file system backed map associating each key with a collection of values
 */
public class FsMultiMap {

    private final Path dir;

    public FsMultiMap(Path dir) {
        this.dir = FsMap.safeCreateDirectories(dir);
    }

    /**
     * @param key
     * @return the collection associated with the given {@code key}; never {@code null}
     */
    public List<String> get(String key) {
        final Path entryDir = dir.resolve(key);
        if (Files.exists(entryDir)) {
            try (Stream<Path> files = Files.list(entryDir)) {
                return files
                        .filter(Files::isRegularFile)
                        .map(f -> f.getFileName().toString())
                        .collect(Collectors.toList());

            } catch (IOException e) {
                throw new RuntimeException("Could not list " + entryDir, e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Add the given {@code value} to the collection associated with the given {@code key}.
     *
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        final Path entryDir = dir.resolve(key);
        FsMap.safeCreateDirectories(entryDir);
        final Path itemPath = entryDir.resolve(value);
        try {
            Files.write(itemPath, value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + itemPath, e);
        }
    }

}
