package io.quarkus.deployment.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.quarkus.runtime.util.ClassPathUtils;

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
        final Set<String> classNames = new LinkedHashSet<>();
        ClassPathUtils.consumeAsStreams(classLoader, fileName, classFile -> {
            try (InputStreamReader reader = new InputStreamReader(classFile, StandardCharsets.UTF_8)) {
                try (BufferedReader br = new BufferedReader(reader)) {
                    readStream(classNames, br);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
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

    /**
     * - Lines starting by a # (or white spaces and a #) are ignored. - For
     * lines containing data before a comment (#) are parsed and only the value
     * before the comment is used.
     */
    private static void readStream(final Set<String> classNames, final BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            int commentMarkerIndex = line.indexOf('#');
            if (commentMarkerIndex >= 0) {
                line = line.substring(0, commentMarkerIndex);
            }
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            classNames.add(line);
        }
    }
}
