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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.shamrock.deployment.ApplicationArchive;
import org.jboss.shamrock.deployment.ApplicationArchiveImpl;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;

/**
 * Class that is responsible for loading application archives from outside the deployment
 */
public class ApplicationArchiveLoader {

    private static final String INDEX_DEPENDENCIES = "index-dependencies";
    private static final String INDEX_JAR = "index-jar";

    public static List<ApplicationArchive> scanForOtherIndexes(ClassLoader classLoader, BuildConfig config) throws IOException {
        ArtifactIndex artifactIndex = new ArtifactIndex(new ClassPathArtifactResolver(classLoader));

        List<BuildConfig.ConfigNode> depList = config.getAll(INDEX_DEPENDENCIES);
        Set<String> depsToIndex = new HashSet<>();
        for (BuildConfig.ConfigNode i : depList) {
            depsToIndex.addAll(i.asStringList());
        }
        List<ApplicationArchive> ret = new ArrayList<>();
        for (String line : depsToIndex) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(":");
            Path path;
            if (parts.length == 2) {
                path = artifactIndex.getPath(parts[0], parts[1], null);
            } else if (parts.length == 3) {
                path = artifactIndex.getPath(parts[0], parts[1], parts[2]);
            } else {
                throw new RuntimeException("Invalid dependencies to index " + line);
            }
            IndexView index = getIndex(path);
            FileSystem fs = FileSystems.newFileSystem(path, classLoader);
            ret.add(new ApplicationArchiveImpl(index, fs.getRootDirectories().iterator().next(), fs));
        }
        ret.addAll(getMarkerIndexes(config, classLoader));
        return ret;
    }


    public static IndexView getIndex(Path path) {
        Indexer indexer = new Indexer();
        try {
            try (JarFile file = new JarFile(path.toFile())) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<ApplicationArchive> getMarkerIndexes(BuildConfig config, ClassLoader classLoader) {
        try {
            List<ApplicationArchive> ret = new ArrayList<>();
            for (Map.Entry<URL, BuildConfig.ConfigNode> dep : config.getDependencyConfig().entrySet()) {
                Boolean node = dep.getValue().get(INDEX_JAR).asBoolean();
                if (node != null && node) {
                    URL url = dep.getKey();
                    if (url.getProtocol().equals("jar")) {
                        Indexer indexer = new Indexer();
                        handleJarPath(url.getPath().substring(5, url.getPath().lastIndexOf('!')), indexer);
                        IndexView index = indexer.complete();
                        FileSystem fs = FileSystems.newFileSystem(url.toURI(), Collections.emptyMap(), classLoader);
                        ret.add(new ApplicationArchiveImpl(index, fs.getRootDirectories().iterator().next(), fs));
                    } else if (url.getProtocol().equals("file")) {
                        int index = url.getPath().lastIndexOf("/META-INF");
                        String pathString = url.getPath().substring(0, index);
                        Path path = Paths.get(pathString);
                        Indexer indexer = new Indexer();
                        handleFilePath(path, indexer);
                        IndexView indexView = indexer.complete();
                        ret.add(new ApplicationArchiveImpl(indexView, path, null));
                    }
                }
            }
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleFilePath(Path path, Indexer indexer) throws IOException {
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
    }

    private static void handleJarPath(String path, Indexer indexer) throws IOException {
        try (JarFile file = new JarFile(path)) {
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
    }
}
