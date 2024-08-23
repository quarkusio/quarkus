package io.quarkus.cli.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Binaries {

    public static Predicate<File> WITH_QUARKUS_PREFIX = f -> f.getName().startsWith("quarkus-");

    private Binaries() {
        //Utility class
    }

    public static Stream<File> streamCommands() {
        return Arrays.stream(System.getenv().getOrDefault("PATH", "").split(File.pathSeparator))
                .map(String::trim)
                .filter(p -> p != null && !p.isEmpty())
                .map(p -> new File(p))
                .filter(File::exists)
                .filter(File::isDirectory)
                .flatMap(d -> Arrays.stream(d.listFiles()))
                .filter(File::isFile)
                .filter(File::canExecute);
    }

    public static Set<File> findCommands(Predicate<File> filter) {
        return streamCommands()
                .filter(filter)
                .collect(Collectors.toSet());
    }

    public static Set<File> findCommands() {
        return findCommands(f -> true);
    }

    public static Set<File> findQuarkusPrefixedCommands() {
        return findCommands(WITH_QUARKUS_PREFIX);
    }

    public static Optional<File> pathOfComamnd(String name) {
        return streamCommands().filter(f -> f.getName().equals(name)).findFirst();
    }
}
