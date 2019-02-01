/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.legacy;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.shamrock.runner.RuntimeRunner;

public class Main {

    public static void main(String... args) {
        if (args.length != 1) {
            System.out.println("Usage: shamrock-legacy.jar ./path/to.war");
            System.exit(1);
        }
        String war = args[0];

        try {
            Path path = Files.createTempDirectory("shamrock-legacy").toAbsolutePath();
            Path lib = path.resolve("lib").toAbsolutePath();
            Files.createDirectories(lib);
            Path archive = path.resolve("archive").toAbsolutePath();
            Files.createDirectories(archive);

            cleanUpOnShutdown(path);
            Set<Path> libraries = new HashSet<>();
            byte[] buf = new byte[1024 * 10];
            //unpack the war, however we don't just unpack it into a normal layout
            //instead we unpack it so that it follows a jar layout rather than a war layout
            //in practice this means:
            //WEB-INF/lib -> lib/
            //WEB-INF/classes -> archive/
            //META-INF -> archive/META-INF/
            //WEB-INF -> archive/WEB-INF
            //everything else -> archive/META-INF/resources

            try (ZipInputStream zip = new ZipInputStream(new FileInputStream(war))) {
                for (ZipEntry i = zip.getNextEntry(); i != null; i = zip.getNextEntry()) {
                    if (i.getName().contains("..")) {
                        throw new RuntimeException("Invalid zip entry " + i.getName());
                    }
                    Path outputLoc;
                    if (i.getName().startsWith("WEB-INF/lib/")) {
                        outputLoc = lib.resolve(i.getName().substring("WEB-INF/lib/".length()));
                        libraries.add(outputLoc);
                    } else if (i.getName().startsWith("WEB-INF/classes/")) {
                        outputLoc = archive.resolve(i.getName().substring("WEB-INF/classes/".length()));
                    } else if (i.getName().startsWith("META-INF/") || i.getName().startsWith("WEB-INF/")) {
                        outputLoc = archive.resolve(i.getName());
                    } else {
                        outputLoc = archive.resolve("META-INF/resources/" + i.getName());
                    }

                    if (i.getName().endsWith("/")) {
                        Files.createDirectories(outputLoc);
                        continue;
                    }
                    Files.createDirectories(outputLoc.getParent());

                    try (FileOutputStream out = new FileOutputStream(outputLoc.toFile())) {
                        int r;
                        while ((r = zip.read(buf)) > 0) {
                            out.write(buf, 0, r);
                        }
                    }
                }
            }

            List<URL> urls = new ArrayList<>();
            List<Path> paths = new ArrayList<>();
            urls.add(archive.toUri().toURL());
            for (Path l : libraries) {
                urls.add(l.toUri().toURL());
                paths.add(l);
            }

            URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[urls.size()]), Main.class.getClassLoader());
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(ucl);
            try(RuntimeRunner runner = new RuntimeRunner(ucl, archive, archive, null, paths)) {
                runner.run();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static void cleanUpOnShutdown(Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Files.walkFileTree(path, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));
    }

}
