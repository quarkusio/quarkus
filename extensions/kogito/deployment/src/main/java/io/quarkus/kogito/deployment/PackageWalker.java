package io.quarkus.kogito.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageWalker {
    public static List<File> getAllSiblings(Collection<File> filesToCompile) {
        return filesToCompile.stream()
                .map(f -> f.getParentFile().toPath())
                .distinct()
                .flatMap(PackageWalker::walk)
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    private static Stream<Path> walk(Path path) {
        try {
            return Files.walk(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
