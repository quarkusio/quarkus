package io.quarkus.deployment.steps;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;

public class CompiledJavaVersionBuildStep {

    /**
     * Determines the Java version by looking up the major version of the first successfully parsed
     * application .class file that is found
     */
    @BuildStep
    public CompiledJavaVersionBuildItem compiledJavaVersion(ApplicationArchivesBuildItem applicationArchivesBuildItem) {
        var rootDirectories = applicationArchivesBuildItem.getRootArchive().getRootDirectories();
        AtomicReference<Integer> majorVersion = new AtomicReference<>(null);
        for (Path path : rootDirectories) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.getFileName().endsWith(".class")) {
                            try (InputStream in = new FileInputStream(file.toFile())) {
                                DataInputStream data = new DataInputStream(in);
                                if (0xCAFEBABE == data.readInt()) {
                                    data.readUnsignedShort(); // minor version -> we don't care about it
                                    majorVersion.set(data.readUnsignedShort());
                                    return FileVisitResult.TERMINATE;
                                }
                            } catch (IOException ignored) {

                            }
                        }
                        // if this was not .class file or there was an error parsing its contents, we continue on to the next file
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {

            }
            if (majorVersion.get() != null) {
                break;
            }
        }
        if (majorVersion.get() == null) {
            return CompiledJavaVersionBuildItem.unknown();
        }
        return CompiledJavaVersionBuildItem.fromMajorJavaVersion(majorVersion.get());
    }
}
