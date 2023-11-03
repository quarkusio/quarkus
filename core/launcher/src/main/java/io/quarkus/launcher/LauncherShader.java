package io.quarkus.launcher;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * This class is used to copy classes and resources from the dependencies of the launcher to the classes directory
 * hiding it under the META-INF from the default classloader.
 * Besides that it collects {@code META-INFO/sisu/javax.inject.Named} resources from the dependencies and merges
 * them into a single file so that the Maven resolver can properly be initialized from the launcher.
 */
public class LauncherShader {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing the source directory and the output directory arguments");
        }
        final Path srcDir = Path.of(args[0]).normalize().toAbsolutePath();
        if (!Files.isDirectory(srcDir)) {
            throw new IllegalArgumentException(srcDir + " is not a directory");
        }
        if (args.length < 2) {
            throw new IllegalArgumentException("Missing the output directory argument");
        }
        final Path destDir = Path.of(args[1]).normalize().toAbsolutePath();
        Files.createDirectories(destDir);

        final Path namedRelative = Path.of("META-INF").resolve("sisu").resolve("javax.inject.Named.ide-launcher-res");
        final Path namedTarget = destDir.resolve(namedRelative);
        Files.createDirectories(namedTarget.getParent());

        try (Stream<Path> depDirs = Files.list(srcDir)) {
            var i = depDirs.iterator();
            try (BufferedWriter namedWriter = Files.newBufferedWriter(namedTarget)) {
                while (i.hasNext()) {
                    final Path unpackedDir = i.next();
                    try (Stream<Path> contentStream = Files.walk(unpackedDir)) {
                        var content = contentStream.iterator();
                        while (content.hasNext()) {
                            final Path p = content.next();
                            if (Files.isDirectory(p)) {
                                continue;
                            }
                            final Path relative = unpackedDir.relativize(p);
                            if (relative.equals(namedRelative)) {
                                for (String s : Files.readAllLines(p)) {
                                    namedWriter.write(s);
                                    namedWriter.newLine();
                                }
                                continue;
                            }
                            final Path destPath = destDir.resolve(relative);
                            if (Files.exists(destPath)) {
                                continue;
                            }
                            Files.createDirectories(destPath.getParent());
                            Files.copy(p, destPath, StandardCopyOption.COPY_ATTRIBUTES);
                        }
                    }
                }
            }
        }
    }
}
