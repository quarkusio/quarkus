package io.quarkus.runner.classloading;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.Set;

public class MemoryClassPathElement implements ClassPathElement {

    private final Map<String, byte[]> resources;

    public MemoryClassPathElement(Map<String, byte[]> resources) {
        this.resources = resources;
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
            };
        }
    }
}
