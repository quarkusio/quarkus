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

package io.quarkus.deployment.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.ApplicationArchiveImpl;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class ApplicationArchiveBuildStep {

    private static final Logger LOGGER = Logger.getLogger(ApplicationArchiveBuildStep.class);

    private static final String JANDEX_INDEX = "META-INF/jandex.idx";

    IndexDependencyConfiguration config;

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static final class IndexDependencyConfiguration {
        /**
         * Artifacts on the class path that should also be indexed, which will allow classes in the index to be
         * processed by quarkuss processors
         */
        @ConfigItem(name = ConfigItem.PARENT)
        Map<String, IndexDependencyConfig> indexDependency;
    }

    @BuildStep
    ApplicationArchivesBuildItem build(ArchiveRootBuildItem root, ApplicationIndexBuildItem appindex,
            List<AdditionalApplicationArchiveMarkerBuildItem> appMarkers) throws IOException {

        Set<String> markerFiles = new HashSet<>();
        for (AdditionalApplicationArchiveMarkerBuildItem i : appMarkers) {
            markerFiles.add(i.getFile());
        }

        List<ApplicationArchive> applicationArchives = scanForOtherIndexes(Thread.currentThread().getContextClassLoader(),
                markerFiles, root.getPath(), Collections.emptyList());
        return new ApplicationArchivesBuildItem(new ApplicationArchiveImpl(appindex.getIndex(), root.getPath(), null),
                applicationArchives);
    }

    private List<ApplicationArchive> scanForOtherIndexes(ClassLoader classLoader, Set<String> applicationArchiveFiles,
            Path appRoot, List<Path> additionalApplicationArchives) throws IOException {
        Set<Path> dependenciesToIndex = new HashSet<>();
        //get paths that are included via index-dependencies
        dependenciesToIndex.addAll(getIndexDependencyPaths(classLoader));
        //get paths that are included via marker files
        Set<String> markers = new HashSet<>(applicationArchiveFiles);
        markers.add(JANDEX_INDEX);
        dependenciesToIndex.addAll(getMarkerFilePaths(classLoader, markers));

        //we don't index the application root, this is handled elsewhere
        dependenciesToIndex.remove(appRoot);

        dependenciesToIndex.addAll(additionalApplicationArchives);

        return indexPaths(dependenciesToIndex, classLoader);
    }

    public List<Path> getIndexDependencyPaths(ClassLoader classLoader) {
        ArtifactIndex artifactIndex = new ArtifactIndex(new ClassPathArtifactResolver(classLoader));
        try {
            List<Path> ret = new ArrayList<>();

            for (Map.Entry<String, IndexDependencyConfig> entry : this.config.indexDependency.entrySet()) {
                Path path;
                if (entry.getValue().classifier.isEmpty()) {
                    path = artifactIndex.getPath(entry.getValue().groupId, entry.getValue().artifactId, null);
                } else {
                    path = artifactIndex.getPath(entry.getValue().groupId, entry.getValue().artifactId,
                            entry.getValue().classifier);
                }
                ret.add(path);
            }
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<ApplicationArchive> indexPaths(Set<Path> dependenciesToIndex, ClassLoader classLoader)
            throws IOException {
        List<ApplicationArchive> ret = new ArrayList<>();

        for (final Path dep : dependenciesToIndex) {
            LOGGER.debugf("Indexing dependency: %s", dep);
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

    private static Collection<? extends Path> getMarkerFilePaths(ClassLoader classLoader, Set<String> applicationArchiveFiles)
            throws IOException {
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
        try {
            if (url.getProtocol().equals("jar")) {
                String jarPath = url.getPath().substring(0, url.getPath().lastIndexOf('!'));
                return Paths.get(new URI(jarPath));
            } else if (url.getProtocol().equals("file")) {
                int index = url.getPath().lastIndexOf("/META-INF");
                String pathString = url.getPath().substring(0, index);
                Path path = Paths.get(new URI(url.getProtocol(), url.getHost(), pathString, null));
                return path;
            }
            throw new RuntimeException("Unkown URL type " + url.getProtocol());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try (Stream<Path> stream = Files.walk(path)) {
            stream.forEach(path1 -> {
                if (path1.toString().endsWith(".class")) {
                    try (FileInputStream in = new FileInputStream(path1.toFile())) {
                        indexer.index(in);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
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
