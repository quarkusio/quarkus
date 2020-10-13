package io.quarkus.devtools;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

public final class ProjectTestUtil {

    private ProjectTestUtil() {
    }

    public static void delete(final File file) throws IOException {

        if (file.exists()) {
            try (Stream<Path> stream = Files.walk(file.toPath())) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }

        Assertions.assertFalse(
                Files.exists(file.toPath()), "Directory still exists");
    }

    public static Consumer<Path> checkContains(String s) {
        return (p) -> assertThat(getContent(p)).contains(s);
    }

    public static String getContent(Path p) {
        return org.assertj.core.util.Files.contentOf(p.toFile(), StandardCharsets.UTF_8);
    }

    public static Consumer<Path> checkMatches(String regex) {
        return (p) -> assertThat(getContent(p)).matches(regex);
    }
}
