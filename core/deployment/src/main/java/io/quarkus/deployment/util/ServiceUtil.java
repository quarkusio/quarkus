package io.quarkus.deployment.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 */
public final class ServiceUtil {
    private ServiceUtil() {
    }

    public static Iterable<Class<?>> classesNamedIn(ClassLoader classLoader, String fileName)
            throws IOException, ClassNotFoundException {
        final ArrayList<Class<?>> list = new ArrayList<>();
        for (String className : classNamesNamedIn(classLoader, fileName)) {
            list.add(Class.forName(className, true, classLoader));
        }
        return Collections.unmodifiableList(list);
    }

    public static Set<String> classNamesNamedIn(ClassLoader classLoader, String fileName) throws IOException {
        final Enumeration<URL> resources = classLoader.getResources(fileName);

        final Set<String> classNames = new LinkedHashSet<>();

        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            try (InputStream is = url.openStream()) {
                try (BufferedInputStream bis = new BufferedInputStream(is)) {
                    try (InputStreamReader isr = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
                        try (BufferedReader br = new BufferedReader(isr)) {
                            readStream(classNames, br);
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableSet(classNames);
    }

    public static Set<String> classNamesNamedIn(Path path) throws IOException {
        final Set<String> set = new LinkedHashSet<>();
        if (!Files.exists(path)) {
            return Collections.emptySet();
        }
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            readStream(set, br);
        } catch (NoSuchFileException | FileNotFoundException e) {
            // unlikely
            return Collections.emptySet();
        }
        return set;
    }

    private static void readStream(final Set<String> classNames, final BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            int commentMarkerIndex = line.indexOf('#');
            if (commentMarkerIndex > 0) {
                line = line.substring(commentMarkerIndex);
            }
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            classNames.add(line);
        }
    }
}
