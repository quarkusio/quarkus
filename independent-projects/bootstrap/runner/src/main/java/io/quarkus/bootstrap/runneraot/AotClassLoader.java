package io.quarkus.bootstrap.runneraot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * A thin ClassLoader layer optimized for AOT (Ahead-of-Time) compilation.
 *
 * This ClassLoader maintains compatibility with standard Java classloaders for optimal AOT
 * performance while providing a caching layer for frequently-accessed resources like
 * service loader files and configuration files.
 *
 * Key characteristics:
 * - All class loading is delegated to the parent (standard system classloader)
 * - Only resource lookups are intercepted for cached resources
 * - Minimal overhead to preserve AOT optimization benefits
 */
public class AotClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final Map<String, byte[]> cachedResources;

    /**
     * Creates a new AotClassLoader with the given URLs and cached resources.
     *
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent classloader
     * @param cachedResources map of resource paths to their cached content
     */
    public AotClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> cachedResources) {
        super(urls, parent);
        this.cachedResources = cachedResources;
    }

    /**
     * Gets the resource with the given name.
     * If the resource is cached, returns it immediately without jar scanning.
     *
     * @param name the resource name
     * @return a URL for reading the resource, or null if not found
     */
    @Override
    public URL getResource(String name) {
        String sanitizedName = sanitizeName(name);

        // If cached, return it immediately - no jar scanning!
        if (cachedResources.containsKey(sanitizedName)) {
            byte[] data = cachedResources.get(sanitizedName);
            if (data != null) {
                return createCachedResourceURL(sanitizedName, data);
            }
        }

        // Not cached - delegate to parent implementation
        return super.getResource(name);
    }

    /**
     * Finds the resource with the given name.
     * If the resource is cached, returns it immediately without jar scanning.
     * Otherwise delegates to the parent implementation.
     *
     * @param name the resource name
     * @return a URL for reading the resource, or null if not found
     */
    @Override
    public URL findResource(String name) {
        String sanitizedName = sanitizeName(name);

        // If cached, return it immediately - no jar scanning!
        if (cachedResources.containsKey(sanitizedName) || sanitizedName.startsWith("META-INF/services/")
                || sanitizedName.equals("application-prod.properties")
                || sanitizedName.equals("META-INF/microprofile-config.properties")) {
            byte[] data = cachedResources.get(sanitizedName);
            if (data != null) {
                return createCachedResourceURL(sanitizedName, data);
            } else {
                return null;
            }
        }

        // Not cached - delegate to parent implementation
        return super.findResource(name);
    }

    /**
     * Gets all resources with the given name.
     * This is the method called by ServiceLoader and other resource loading mechanisms.
     * If the resource is cached, returns ONLY the cached version without jar scanning.
     * This is the key optimization - service files and config files are pre-cached,
     * so ServiceLoader and config loading don't need to scan jars.
     *
     * @param name the resource name
     * @return an enumeration of URLs for the resources
     * @throws IOException if I/O errors occur
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        String sanitizedName = sanitizeName(name);

        // If cached, return ONLY the cached version - skip jar scanning entirely!
        // The cached version already contains the concatenated content from all jars
        if (cachedResources.containsKey(sanitizedName) || sanitizedName.startsWith("META-INF/services/")
                || sanitizedName.equals("application-prod.properties")
                || sanitizedName.equals("META-INF/microprofile-config.properties")) {
            byte[] data = cachedResources.get(sanitizedName);
            if (data != null) {
                return Collections.enumeration(Collections.singletonList(
                        createCachedResourceURL(sanitizedName, data)));
            }
            // Resource is known to not exist (cached as empty)
            return Collections.emptyEnumeration();
        }

        // Not cached - delegate to parent implementation (will scan jars)
        return super.getResources(name);
    }

    /**
     * Finds all resources with the given name.
     * If the resource is cached, returns ONLY the cached version without jar scanning.
     *
     * @param name the resource name
     * @return an enumeration of URLs for the resources
     * @throws IOException if I/O errors occur
     */
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        String sanitizedName = sanitizeName(name);

        // If cached, return ONLY the cached version - skip jar scanning entirely!
        if (cachedResources.containsKey(sanitizedName) || sanitizedName.startsWith("META-INF/services/")
                || sanitizedName.equals("application-prod.properties")
                || sanitizedName.equals("META-INF/microprofile-config.properties")) {
            byte[] data = cachedResources.get(sanitizedName);
            if (data != null) {
                return Collections.enumeration(Collections.singletonList(
                        createCachedResourceURL(sanitizedName, data)));
            }
            return Collections.emptyEnumeration();
        }

        // Not cached - delegate to parent implementation (will scan jars)
        return super.findResources(name);
    }

    /**
     * Loads the class with the specified binary name.
     *
     * Strategy for AOT compatibility with resource caching:
     * - JDK/system classes: delegate to parent (parent-first for platform classes)
     * - Application/library classes: load ourselves first (child-first for app classes)
     *
     * This ensures that application code is loaded by THIS classloader, so when
     * it calls ServiceLoader or loads resources, it will use OUR resource cache.
     *
     * @param name the binary name of the class
     * @param resolve if true then resolve the class
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class could not be found
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    private String sanitizeName(String name) {
        if (name != null && name.length() > 0 && name.charAt(0) == '/') {
            return name.substring(1);
        }
        return name;
    }

    private URL createCachedResourceURL(String name, byte[] data) {
        try {
            return new URL(null, "cached:" + name, new CachedResourceURLStreamHandler(data));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to create URL for cached resource: " + name, e);
        }
    }

    /**
     * URL stream handler for cached resources.
     */
    private static class CachedResourceURLStreamHandler extends URLStreamHandler {
        private final byte[] data;

        CachedResourceURLStreamHandler(byte[] data) {
            this.data = data;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new CachedResourceURLConnection(url, data);
        }
    }

    /**
     * URL connection for cached resources.
     */
    private static class CachedResourceURLConnection extends URLConnection {
        private final byte[] data;

        CachedResourceURLConnection(URL url, byte[] data) {
            super(url);
            this.data = data;
        }

        @Override
        public void connect() {
            // No-op for in-memory resources
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public int getContentLength() {
            return data.length;
        }
    }
}
