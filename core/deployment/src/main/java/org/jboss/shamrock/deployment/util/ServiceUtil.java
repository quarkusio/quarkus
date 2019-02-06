package org.jboss.shamrock.deployment.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 */
public final class ServiceUtil {
    private ServiceUtil() {}

    public static Iterable<Class<?>> classesNamedIn(ClassLoader classLoader, String fileName) throws IOException, ClassNotFoundException {
        final ArrayList<Class<?>> list = new ArrayList<>();
        final Enumeration<URL> resources = classLoader.getResources(fileName);
        final Set<Class<?>> found = new HashSet<>();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            try (InputStream is = url.openStream()) {
                try (BufferedInputStream bis = new BufferedInputStream(is)) {
                    try (InputStreamReader isr = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
                        try (BufferedReader br = new BufferedReader(isr)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                line = line.trim();
                                final Class<?> clazz = Class.forName(line, true, classLoader);
                                if (found.add(clazz)) {
                                    list.add(clazz);
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }
}
