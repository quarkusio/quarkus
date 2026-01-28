package io.quarkus.bootstrap.runner;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;
import static io.quarkus.commons.classloading.ClassLoaderHelper.isInJdkPackage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * A ClassLoader that behaves similarly to the RunnerClassLoader for resources
 * but delegates all class loading to the JDK application class loader.
 *
 * The idea is that it lets us optimize the resource case, while still benefiting from AOT.
 * Our hope is that we will be able to use a fully custom ClassLoader with Project Leyden at some point but we are not there
 * yet.
 *
 * Key characteristics:
 * - All class loading is delegated to the parent
 * - Resource lookups are intercepted for the service files and otherwise delegated to the parent
 * - For some selected directories, we have a full index of the content so that we can avoid negative lookups in the parent
 *
 * Note that not all ServiceLoader calls will hit this ClassLoader as some low levels one are directly hitting
 * the ClassLoader used to load the classes, thus circumventing the optimizations we have in this ClassLoader.
 * It is an expected behavior though, and will be vastly improved once we can use a custom ClassLoader with Leyden.
 */
public class AotRunnerClassLoader extends ClassLoader {

    static {
        registerAsParallelCapable();
    }

    // the following two fields go hand in hand - they need to both be populated from the same data
    // in order for the resource loading to work properly
    private final Set<String> fullyIndexedDirectories;
    private final Set<String> fullyIndexedResources;

    private final Map<String, byte[]> serviceFiles;

    AotRunnerClassLoader(ClassLoader parent, Set<String> fullyIndexedDirectories, Set<String> fullyIndexedResources,
            Map<String, byte[]> serviceFiles) {
        super(parent);
        this.fullyIndexedDirectories = fullyIndexedDirectories;
        this.fullyIndexedResources = fullyIndexedResources;
        this.serviceFiles = serviceFiles;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // fast path for JDK classes
        if (isInJdkPackage(name)) {
            return getParent().loadClass(name);
        }

        // this infrastructure is not used for classes at the moment but could be in the future
        String classResource = fromClassNameToResourceName(name);
        String dirName = getDirNameFromResourceName(classResource);
        if (fullyIndexedDirectories.contains(dirName) && !fullyIndexedResources.contains(classResource)) {
            throw new ClassNotFoundException(name);
        }

        return getParent().loadClass(name);
    }

    /**
     * This method is needed to make packages work correctly on JDK9+, as it will be called
     * to load the package-info class.
     */
    @Override
    protected Class<?> findClass(String moduleName, String name) {
        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public URL getResource(String name) {
        name = sanitizeName(name);

        // If cached, return it immediately, no need to scan the jars
        if (serviceFiles.containsKey(name)) {
            byte[] data = serviceFiles.get(name);
            if (data != null) {
                return createCachedResourceURL(name, data);
            }
        }

        String dirName = getDirNameFromResourceName(name);
        if (fullyIndexedDirectories.contains(dirName) && !fullyIndexedResources.contains(name)) {
            return null;
        }

        // if it exists, and is not cached, then delegate to the parent
        return super.getResource(name);
    }

    @Override
    protected URL findResource(String name) {
        name = sanitizeName(name);

        // If cached, return it immediately, no need to scan the jars
        if (serviceFiles.containsKey(name)) {
            byte[] data = serviceFiles.get(name);
            if (data != null) {
                return createCachedResourceURL(name, data);
            }
        }

        String dirName = getDirNameFromResourceName(name);
        if (fullyIndexedDirectories.contains(dirName) && !fullyIndexedResources.contains(name)) {
            return null;
        }

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
        name = sanitizeName(name);

        // The cached version already contains the concatenated content from all jars
        if (serviceFiles.containsKey(name)) {
            byte[] data = serviceFiles.get(name);
            if (data != null) {
                return Collections.enumeration(Collections.singletonList(
                        createCachedResourceURL(name, data)));
            }
            return Collections.emptyEnumeration();
        }

        String dirName = getDirNameFromResourceName(name);
        if (fullyIndexedDirectories.contains(dirName) && !fullyIndexedResources.contains(name)) {
            return Collections.emptyEnumeration();
        }

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
        name = sanitizeName(name);

        // The cached version already contains the concatenated content from all jars
        if (serviceFiles.containsKey(name)) {
            byte[] data = serviceFiles.get(name);
            if (data != null) {
                return Collections.enumeration(Collections.singletonList(
                        createCachedResourceURL(name, data)));
            }
            return Collections.emptyEnumeration();
        }

        String dirName = getDirNameFromResourceName(name);
        if (fullyIndexedDirectories.contains(dirName) && !fullyIndexedResources.contains(name)) {
            return Collections.emptyEnumeration();
        }

        // Not cached - delegate to parent implementation (will scan jars)
        return super.findResources(name);
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

    private static String sanitizeName(String name) {
        if (name != null && !name.isEmpty() && name.charAt(0) == '/') {
            return name.substring(1);
        }
        return name;
    }

    private static String getDirNameFromResourceName(String resourceName) {
        final int index = resourceName.lastIndexOf('/');
        if (index == -1) {
            // Return empty string for resources in the root directory (no package/directory)
            return "";
        }
        return resourceName.substring(0, index);
    }

    @Override
    public boolean equals(Object o) {
        //see comment in hashCode
        return this == o;
    }

    @Override
    public int hashCode() {
        //We can return a constant as we expect to have a single instance of these;
        //this is useful to avoid triggering a call to the identity hashcode,
        //which could be rather inefficient as there's good chances that some component
        //will have inflated the monitor of this instance.
        //A hash collision would be unfortunate but unexpected, and shouldn't be a problem
        //as the equals implementation still does honour the identity contract .
        //See also discussion on https://github.com/smallrye/smallrye-context-propagation/pull/443
        return 1;
    }
}
