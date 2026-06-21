package io.quarkus.maven.worker;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects the classpath URLs from a Maven plugin classloader hierarchy.
 */
public final class PluginClassPath {

    private PluginClassPath() {
    }

    public static String from(ClassLoader classLoader) {
        Set<String> paths = new LinkedHashSet<>();
        collect(classLoader, paths);
        return String.join(File.pathSeparator, paths);
    }

    private static void collect(ClassLoader classLoader, Set<String> paths) {
        ClassLoader current = classLoader;
        while (current != null) {
            for (URL url : urls(current)) {
                paths.add(toLocalPath(url));
            }
            current = current.getParent();
        }
    }

    private static List<URL> urls(ClassLoader classLoader) {
        List<URL> urls = new ArrayList<>();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            for (URL url : urlClassLoader.getURLs()) {
                urls.add(url);
            }
        }
        try {
            var method = classLoader.getClass().getMethod("getURLs");
            Object value = method.invoke(classLoader);
            if (value instanceof URL[]) {
                URL[] classRealmUrls = (URL[]) value;
                for (URL url : classRealmUrls) {
                    urls.add(url);
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // not a ClassRealm-like loader
        }
        return urls;
    }

    private static String toLocalPath(URL url) {
        try {
            return Path.of(url.toURI()).toString();
        } catch (Exception e) {
            return url.getPath();
        }
    }
}
