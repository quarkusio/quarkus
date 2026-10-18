package io.quarkus.bootstrap.runner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

interface JarVisitor {

    static void visitJar(Path jar, JarVisitor... jarVisitors) throws IOException {
        for (JarVisitor jarVisitor : jarVisitors) {
            jarVisitor.preVisit(jar);
        }

        if (Files.isDirectory(jar)) {
            // this can only really happen when testing quarkus itself
            // but is included for completeness
            Files.walkFileTree(jar, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    for (JarVisitor jarVisitor : jarVisitors) {
                        jarVisitor.visitRegularDirectory(jar, dir, jar.relativize(dir).toString().replace('\\', '/'));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    for (JarVisitor jarVisitor : jarVisitors) {
                        jarVisitor.visitRegularFile(jar, file, jar.relativize(file).toString().replace('\\', '/'));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                for (JarVisitor jarVisitor : jarVisitors) {
                    Manifest manifest = jarFile.getManifest();
                    if (manifest != null) {
                        jarVisitor.visitJarManifest(jar, jarFile.getManifest());
                    }
                }

                Enumeration<? extends ZipEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    if (entry.getName().isEmpty()) {
                        continue;
                    }

                    if (entry.isDirectory()) {
                        for (JarVisitor jarVisitor : jarVisitors) {
                            jarVisitor.visitJarDirectoryEntry(jarFile, entry);
                        }
                    } else {
                        for (JarVisitor jarVisitor : jarVisitors) {
                            jarVisitor.visitJarFileEntry(jarFile, entry);
                        }
                    }
                }
            }
        }
    }

    default void preVisit(Path jar) {
    }

    default void visitRegularDirectory(Path jar, Path directory, String relativePath) {
    }

    default void visitRegularFile(Path jar, Path file, String relativePath) {
    }

    default void visitJarManifest(Path jar, Manifest manifest) {
    }

    default void visitJarDirectoryEntry(JarFile jarFile, ZipEntry directoryEntry) {
    }

    default void visitJarFileEntry(JarFile jarFile, ZipEntry fileEntry) {
    }
}
