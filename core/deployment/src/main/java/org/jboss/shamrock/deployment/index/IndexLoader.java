package org.jboss.shamrock.deployment.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;

/**
 * Class that is responsible for loading indexes from outside the deployment
 */
public class IndexLoader {

    private static final String INDEX_DEPENDENCIES = "index-dependencies";
    private static final String INDEX_JAR = "index-jar";
    private static final String INDEX_PACKAGES = "index-packages";

    public static List<IndexView> scanForOtherIndexes(ClassLoader classLoader, BuildConfig config) throws IOException {
        ArtifactIndex artifactIndex = new ArtifactIndex(new ClassPathArtifactResolver(classLoader));

        List<BuildConfig.ConfigNode> depList = config.getAll(INDEX_DEPENDENCIES);
        Set<String> depsToIndex = new HashSet<>();
        for (BuildConfig.ConfigNode i : depList) {
            depsToIndex.addAll(i.asStringList());
        }
        List<IndexView> ret = new ArrayList<>();
        for (String line : depsToIndex) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(":");

            if (parts.length == 2) {
                ret.add(artifactIndex.getIndex(parts[0], parts[1], null));
            } else if (parts.length == 3) {
                ret.add(artifactIndex.getIndex(parts[0], parts[1], parts[2]));
            } else {
                throw new RuntimeException("Invalid dependencies to index " + line);
            }
        }
        ret.add(getMarkerIndexes(config));
        ret.add(getPackageIndexes(classLoader, config, CompositeIndex.create(new ArrayList<>(ret))));

        return ret;
    }

    private static IndexView getPackageIndexes(ClassLoader classLoader, BuildConfig config, CompositeIndex compositeIndex) throws IOException {

        List<BuildConfig.ConfigNode> packageList = config.getAll(INDEX_PACKAGES);
        Set<String> packagesToIndex = new HashSet<>();
        for (BuildConfig.ConfigNode i : packageList) {
            packagesToIndex.addAll(i.asStringList());
        }
        Indexer indexer = new Indexer();
        for(String pkg : packagesToIndex) {
            Enumeration<URL> urls = classLoader.getResources(pkg.replace(".", "/"));
            while (urls.hasMoreElements()) {
                System.out.println(urls.nextElement());
            }
        }
        return indexer.complete();
    }

    public static IndexView getMarkerIndexes(BuildConfig config) {
        try {
            Indexer indexer = new Indexer();
            for (Map.Entry<URL, BuildConfig.ConfigNode> dep : config.getDependencyConfig().entrySet()) {
                Boolean node = dep.getValue().get(INDEX_JAR).asBoolean();
                if (node != null && node) {
                    URL url = dep.getKey();
                    if (url.getProtocol().equals("jar")) {
                        handleJarPath(url.getPath().substring(5, url.getPath().lastIndexOf('!')), indexer);
                    } else if (url.getProtocol().equals("file")) {
                        int index = url.getPath().lastIndexOf("/META-INF");
                        handleFilePath(url.getPath().substring(0, index), indexer);
                    }
                }
            }
            return indexer.complete();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleFilePath(String path, Indexer indexer) throws IOException {
        Files.walk(Paths.get(path)).forEach(new Consumer<Path>() {
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
