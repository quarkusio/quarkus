/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.creator.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class ZipUtils {

    private static final String JAR_URI_PREFIX = "jar:";
    private static final Map<String, String> CREATE_ENV = Collections.singletonMap("create", "true");

    public static void unzip(Path zipFile, Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        try (FileSystem zipfs = newFileSystem(zipFile)) {
            for (Path zipRoot : zipfs.getRootDirectories()) {
                copyFromZip(zipRoot, targetDir);
            }
        }
    }

    public static URI toZipUri(Path zipFile) throws IOException {
        URI zipUri = zipFile.toUri();
        try {
            zipUri = new URI(JAR_URI_PREFIX + zipUri.getScheme(), zipUri.getPath(), null);
        } catch (URISyntaxException e) {
            throw new IOException("Failed to create a JAR URI for " + zipFile, e);
        }
        return zipUri;
    }

    public static void copyFromZip(Path source, Path target) throws IOException {
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        final Path targetDir = target.resolve(source.relativize(dir).toString());
                        try {
                            Files.copy(dir, targetDir);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isDirectory(targetDir))
                                throw e;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Files.copy(file, target.resolve(source.relativize(file).toString()),
                                StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    public static void zip(Path src, Path zipFile) throws IOException {
        try (FileSystem zipfs = newZip(zipFile)) {
            if (Files.isDirectory(src)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(src)) {
                    for (Path srcPath : stream) {
                        copyToZip(src, srcPath, zipfs);
                    }
                }
            } else {
                Files.copy(src, zipfs.getPath(src.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static FileSystem newZip(Path zipFile) throws IOException {
        return newFileSystem(toZipUri(zipFile), Files.exists(zipFile) ? Collections.emptyMap() : CREATE_ENV);
    }

    private static void copyToZip(Path srcRoot, Path srcPath, FileSystem zipfs) throws IOException {
        Files.walkFileTree(srcPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        final Path targetDir = zipfs.getPath(srcRoot.relativize(dir).toString());
                        try {
                            Files.copy(dir, targetDir);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isDirectory(targetDir)) {
                                throw e;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Files.copy(file, zipfs.getPath(srcRoot.relativize(file).toString()),
                                StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    /**
     * This call is not thread safe, a single of FileSystem can be created for the
     * profided uri until it is closed.
     * 
     * @param uri The uri to the zip file.
     * @param env Env map.
     * @return A new FileSystem.
     * @throws IOException in case of a failure
     */
    public static FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        // If Multi threading required, logic should be added to wrap this fs
        // onto a fs that handles a reference counter and close the fs only when all thread are done
        // with it.
        return FileSystems.newFileSystem(uri, env);
    }

    /**
     * This call is thread safe, a new FS is created for each invocation.
     * 
     * @param path The zip file.
     * @return A new FileSystem instance
     * @throws IOException in case of a failure
     */
    public static FileSystem newFileSystem(Path path) throws IOException {
        return FileSystems.newFileSystem(path, null);
    }
}
