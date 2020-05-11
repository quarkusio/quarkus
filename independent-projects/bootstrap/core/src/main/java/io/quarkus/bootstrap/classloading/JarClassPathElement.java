package io.quarkus.bootstrap.classloading;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.jboss.logging.Logger;

/**
 * A class path element that represents a file on the file system
 */
public class JarClassPathElement implements ClassPathElement {

    private static final Logger log = Logger.getLogger(JarClassPathElement.class);
    private final File file;
    private final URL jarPath;
    private final Path root;
    private JarFile jarFile;
    private boolean closed;

    public JarClassPathElement(Path root) {
        try {
            jarPath = root.toUri().toURL();
            this.root = root;
            jarFile = new JarFile(file = root.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Error while reading file as JAR: " + root, e);
        }
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public synchronized ClassPathResource getResource(String name) {
        return withJarFile(new Function<JarFile, ClassPathResource>() {
            @Override
            public ClassPathResource apply(JarFile jarFile) {
                ZipEntry res = jarFile.getEntry(name);
                if (res != null) {
                    return new ClassPathResource() {
                        @Override
                        public ClassPathElement getContainingElement() {
                            return JarClassPathElement.this;
                        }

                        @Override
                        public String getPath() {
                            return name;
                        }

                        @Override
                        public URL getUrl() {
                            try {
                                return new URL("jar", null, jarPath.getProtocol() + ":" + jarPath.getPath() + "!/" + name);
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public byte[] getData() {
                            return withJarFile(new Function<JarFile, byte[]>() {
                                @Override
                                public byte[] apply(JarFile jarFile) {
                                    try {
                                        try {
                                            return readStreamContents(jarFile.getInputStream(res));
                                        } catch (InterruptedIOException e) {
                                            //if we are interrupted reading data we finish the op, then just re-interrupt the thread state
                                            byte[] bytes = readStreamContents(jarFile.getInputStream(res));
                                            Thread.currentThread().interrupt();
                                            return bytes;
                                        }
                                    } catch (IOException e) {
                                        throw new RuntimeException("Unable to read " + name, e);
                                    }
                                }
                            });
                        }
                    };
                }
                return null;

            }
        });
    }

    private <T> T withJarFile(Function<JarFile, T> func) {
        if (closed) {
            //we still need this to work if it is closed, so shutdown hooks work
            //once it is closed it simply does not hold on to any resources
            try (JarFile jarFile = new JarFile(file)) {
                return func.apply(jarFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return func.apply(jarFile);
        }
    }

    @Override
    public synchronized Set<String> getProvidedResources() {
        return withJarFile((new Function<JarFile, Set<String>>() {
            @Override
            public Set<String> apply(JarFile jarFile) {
                Set<String> paths = new HashSet<>();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    paths.add(entry.getName());
                }
                return paths;
            }
        }));
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        URL url = null;
        try {
            URI uri = new URI("jar:file", null, jarPath.getPath() + "!/", null);
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + jarPath, e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, null, classLoader, null);
        return protectionDomain;
    }

    @Override
    public Manifest getManifest() {
        return withJarFile(new Function<JarFile, Manifest>() {
            @Override
            public Manifest apply(JarFile jarFile) {
                try {
                    return jarFile.getManifest();
                } catch (IOException e) {
                    log.warnf("Failed to parse manifest for %s", jarPath);
                    return null;
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        closed = true;
        jarFile.close();
    }

    public static byte[] readStreamContents(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[10000];
            int r;
            while ((r = inputStream.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return out.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    @Override
    public String toString() {
        return file.getName() + ": " + jarPath;
    }
}
