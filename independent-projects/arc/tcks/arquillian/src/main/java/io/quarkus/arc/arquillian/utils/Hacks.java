package io.quarkus.arc.arquillian.utils;

import java.net.URLConnection;

public class Hacks {
    public static void preventFileHandleLeaks() {
        // `JarURLConnection` is known to leak file handles with caching enabled,
        // which occurs in `URLClassLoader` and in CDI TCK's `PropertiesBasedConfigurationBuilder`
        // and causes failures during directory deletion on Windows
        URLConnection.setDefaultUseCaches("jar", false);
    }
}
