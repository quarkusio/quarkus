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

package org.jboss.shamrock.deployment.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.shamrock.deployment.ApplicationArchive;
import org.jboss.shamrock.deployment.ApplicationArchiveImpl;

/**
 * Class that is responsible for loading application archives from outside the deployment
 */
public class ApplicationArchiveLoader {

    private static final String INDEX_DEPENDENCIES = "index-dependencies";
    private static final String INDEX_JAR = "index-jar";

    private static final String JANDEX_INDEX = "META-INF/jandex.idx";

    public static List<ApplicationArchive> scanForOtherIndexes(ClassLoader classLoader, Set<String> applicationArchiveFiles, Path appRoot, List<Path> additionalApplicationArchives) throws IOException {


        Set<Path> dependenciesToIndex = new HashSet<>();

        //get paths that are included via marker files
        Set<String> markers = new HashSet<>(applicationArchiveFiles);
        markers.add(JANDEX_INDEX);
        dependenciesToIndex.addAll(getMarkerFilePaths(classLoader, markers));

        //we don't index the application root, this is handled elsewhere
        dependenciesToIndex.remove(appRoot);

        dependenciesToIndex.addAll(additionalApplicationArchives);

        return indexPaths(dependenciesToIndex, classLoader);
    }

    private static List<ApplicationArchive> indexPaths(Set<Path> dependenciesToIndex, ClassLoader classLoader) throws IOException {
        List<ApplicationArchive> ret = new ArrayList<>();

        for (final Path dep : dependenciesToIndex) {
            if (Files.isDirectory(dep)) {
                IndexView indexView = handleFilePath(dep);
                ret.add(new ApplicationArchiveImpl(indexView, dep, null));
            } else {
                IndexView index = handleJarPath(dep);
                FileSystem fs = FileSystems.newFileSystem(dep, classLoader);
                ret.add(new ApplicationArchiveImpl(index, fs.getRootDirectories().iterator().next(), fs));
            }
        }


        return ret;
    }

    private static Collection<? extends Path> getMarkerFilePaths(ClassLoader classLoader, Set<String> applicationArchiveFiles) throws IOException {
        List<Path> ret = new ArrayList<>();
        for (String file : applicationArchiveFiles) {
            Enumeration<URL> e = classLoader.getResources(file);
            while (e.hasMoreElements()) {
                URL url = e.nextElement();
                ret.add(urlToPath(url));
            }
        }

        return ret;
    }


    private static Path urlToPath(URL url) {
        if (url.getProtocol().equals("jar")) {
            String jarPath = url.getPath().substring(5, url.getPath().lastIndexOf('!'));
            return Paths.get(jarPath);
        } else if (url.getProtocol().equals("file")) {
            int index = url.getPath().lastIndexOf("/META-INF");
            String pathString = url.getPath().substring(0, index);
            Path path = Paths.get(pathString);
            return path;
        }
        throw new RuntimeException("Unkown URL type " + url.getProtocol());
    }


    private static Index handleFilePath(Path path) throws IOException {
        Path existing = path.resolve(JANDEX_INDEX);
        if (Files.exists(existing)) {
            try (FileInputStream in = new FileInputStream(existing.toFile())) {
                IndexReader r = new IndexReader(in);
                return r.read();
            }
        }

        Indexer indexer = new Indexer();
        Files.walk(path).forEach(new Consumer<Path>() {
            @Override
            public void accept(Path path) {
                if (path.toString().endsWith(".class")) {
                    try (FileInputStream in = new FileInputStream(path.toFile())) {
                        indexer.index(in);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        return indexer.complete();
    }

    private static Index handleJarPath(Path path) throws IOException {
        Indexer indexer = new Indexer();
        try (JarFile file = new JarFile(path.toFile())) {
            ZipEntry existing = file.getEntry(JANDEX_INDEX);
            if (existing != null) {
                try (InputStream in = file.getInputStream(existing)) {
                    IndexReader r = new IndexReader(in);
                    return r.read();
                }
            }

            Enumeration<JarEntry> e = file.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream inputStream = file.getInputStream(entry)) {
                        indexer.index(inputStream);
                    }
                }
            }
        }
        return indexer.complete();
    }
}
