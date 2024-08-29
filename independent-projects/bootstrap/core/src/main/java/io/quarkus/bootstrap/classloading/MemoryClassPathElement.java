package io.quarkus.bootstrap.classloading;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.OpenPathTree;

public class MemoryClassPathElement extends AbstractClassPathElement {

    private static final ProtectionDomain NULL_PROTECTION_DOMAIN = new ProtectionDomain(
            new CodeSource(null, (Certificate[]) null), null);

    private volatile Map<String, byte[]> resources;
    private volatile long lastModified = System.currentTimeMillis();
    private final boolean runtime;

    public MemoryClassPathElement(Map<String, byte[]> resources, boolean runtime) {
        this.resources = resources;
        this.runtime = runtime;
    }

    @Override
    public boolean isRuntime() {
        return runtime;
    }

    public void reset(Map<String, byte[]> resources) {
        Map<String, byte[]> newResources = new HashMap<>(resources);
        //we can't delete .class files from the loader
        //gizmo may not generate the same function names on restart
        //so if we delete them already loaded classes may have problems, as functions they reference
        //may have been removed
        //see https://github.com/quarkusio/quarkus/issues/8301
        for (Map.Entry<String, byte[]> e : this.resources.entrySet()) {
            if (newResources.containsKey(e.getKey())) {
                continue;
            }
            if (e.getKey().endsWith(".class")) {
                newResources.put(e.getKey(), e.getValue());
            }
        }
        this.resources = newResources;
        lastModified = System.currentTimeMillis();
    }

    @Override
    public Path getRoot() {
        return null;
    }

    @Override
    public <T> T apply(Function<OpenPathTree, T> func) {
        return func.apply(EmptyPathTree.getInstance());
    }

    @Override
    public ClassPathResource getResource(String name) {
        byte[] res = resources.get(name);
        if (res == null) {
            return null;
        }
        return new ClassPathResource() {
            @Override
            public ClassPathElement getContainingElement() {
                return MemoryClassPathElement.this;
            }

            @Override
            public String getPath() {
                return name;
            }

            @Override
            public URL getUrl() {
                String path = "quarkus:" + name;
                try {
                    URL url = new URL(null, path, new MemoryUrlStreamHandler(resources.get(name), lastModified));

                    return url;
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL: " + path);
                }
            }

            @Override
            public byte[] getData() {
                return res;
            }

            @Override
            public boolean isDirectory() {
                return false;
            }
        };
    }

    @Override
    public Set<String> getProvidedResources() {
        return resources.keySet();
    }

    @Override
    public boolean containsReloadableResources() {
        return true;
    }

    @Override
    public ProtectionDomain getProtectionDomain() {
        // we used to include the class bytes in the ProtectionDomain
        // but it is not a good idea
        // see https://github.com/quarkusio/quarkus/issues/41417 for more details about the problem
        return NULL_PROTECTION_DOMAIN;
    }

    @Override
    public void close() throws IOException {

    }

    private static class MemoryUrlStreamHandler extends URLStreamHandler {
        private final byte[] bytes;
        private long lastModified;

        public MemoryUrlStreamHandler(byte[] bytes, long lastModified) {
            this.bytes = bytes;
            this.lastModified = lastModified;
        }

        @Override
        protected URLConnection openConnection(final URL u) throws IOException {
            return new URLConnection(u) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public long getLastModified() {
                    return lastModified;
                }

                @Override
                public int getContentLength() {
                    return bytes.length;
                }

                @Override
                public long getContentLengthLong() {
                    return bytes.length;
                }
            };
        }
    }
}
