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

public class MemoryClassPathElement extends AbstractClassPathElement {

    private volatile Map<String, byte[]> resources;
    private volatile long lastModified = System.currentTimeMillis();

    public MemoryClassPathElement(Map<String, byte[]> resources) {
        this.resources = resources;
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
                    URL url = new URL(null, path, new MemoryUrlStreamHandler(name));

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
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        URL url = null;
        try {
            url = new URL(null, "quarkus:/", new MemoryUrlStreamHandler("quarkus:/"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for memory element", e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, null, classLoader, null);
        return protectionDomain;
    }

    @Override
    public void close() throws IOException {

    }

    private class MemoryUrlStreamHandler extends URLStreamHandler {
        private final String name;

        public MemoryUrlStreamHandler(String name) {
            this.name = name;
        }

        @Override
        protected URLConnection openConnection(final URL u) throws IOException {
            return new URLConnection(u) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(resources.get(name));
                }

                @Override
                public long getLastModified() {
                    return lastModified;
                }

                @Override
                public int getContentLength() {
                    return resources.get(name).length;
                }

                @Override
                public long getContentLengthLong() {
                    return resources.get(name).length;
                }
            };
        }
    }
}
