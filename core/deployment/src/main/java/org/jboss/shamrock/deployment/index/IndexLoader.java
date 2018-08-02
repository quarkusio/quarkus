package org.jboss.shamrock.deployment.index;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

/**
 * Class that is responsible for loading indexes from outside the deployment
 */
public class IndexLoader {


    private static final String META_INF_SHAMROCK_INDEX_DEPENDENCIES = "META-INF/shamrock-index-dependencies";
    static final String INDEX_MARKER = "META-INF/shamrock-index.marker";

    public static List<IndexView> scanForOtherIndexes(ClassLoader classLoader) throws IOException {
        ArtifactIndex artifactIndex = new ArtifactIndex(new ClassPathArtifactResolver(classLoader));
        Enumeration<URL> resources = classLoader.getResources(META_INF_SHAMROCK_INDEX_DEPENDENCIES);
        List<IndexView> ret = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(resource.openStream()))) {
                String line = in.readLine();
                if (line.startsWith("#")) {
                    continue;
                }
                int commentPos = line.indexOf('#');
                if (commentPos > 0) {
                    line = line.substring(0, commentPos);
                }
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
            } catch (Exception e) {
                throw new RuntimeException("Exception resolving dependencies from " + resource, e);
            }
        }
        ret.add(getMarkerIndexes(classLoader));
        return ret;
    }

    public static IndexView getMarkerIndexes(ClassLoader classLoader) {
        try {
            Indexer indexer = new Indexer();
            Enumeration<URL> urls = classLoader.getResources(INDEX_MARKER);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getProtocol().equals("jar")) {
                    handleJarPath(url.getPath().substring(5, url.getPath().length() - INDEX_MARKER.length() - 2), indexer);
                } else if (url.getProtocol().equals("file")) {
                    handleFilePath(url.getPath().substring(0, url.getPath().length() - INDEX_MARKER.length() - 1), indexer);
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
