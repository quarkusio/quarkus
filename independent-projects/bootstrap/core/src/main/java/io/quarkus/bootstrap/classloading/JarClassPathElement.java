package io.quarkus.bootstrap.classloading;

import io.smallrye.common.io.jar.JarEntries;
import io.smallrye.common.io.jar.JarFiles;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jboss.logging.Logger;

/**
 * A class path element that represents a file on the file system
 */
public class JarClassPathElement implements ClassPathElement {

    public static final int JAVA_VERSION;

    static {
        int version = 8;
        try {
            Method versionMethod = Runtime.class.getMethod("version");
            Object v = versionMethod.invoke(null);
            List<Integer> list = (List<Integer>) v.getClass().getMethod("version").invoke(v);
            version = list.get(0);
        } catch (Exception e) {
            //version 8
        }
        JAVA_VERSION = version;
        //force this class to be loaded
        //if quarkus is recompiled it needs to have already
        //been loaded
        //this is just a convenience for quarkus devs that means exit
        //should work properly if you recompile while quarkus is running
        new ZipFileMayHaveChangedException(null);
    }

    private static final Logger log = Logger.getLogger(JarClassPathElement.class);
    public static final String META_INF_VERSIONS = "META-INF/versions/";

    private final File file;
    private final URL jarPath;
    private final Path root;
    private final Lock readLock;
    private final Lock writeLock;

    //Closing the jarFile requires the exclusive lock, while reading data from the jarFile requires the shared lock.
    private final JarFile jarFile;
    private volatile boolean closed;

    public JarClassPathElement(Path root) {
        try {
            jarPath = root.toUri().toURL();
            this.root = root;
            jarFile = JarFiles.create(file = root.toFile());
            ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
            this.readLock = readWriteLock.readLock();
            this.writeLock = readWriteLock.writeLock();
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
                JarEntry res = jarFile.getJarEntry(name);
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
                                String realName = JarEntries.getRealName(res);
                                // Avoid ending the URL with / to avoid breaking compatibility
                                if (realName.endsWith("/")) {
                                    realName = realName.substring(0, realName.length() - 1);
                                }
                                String urlFile = jarPath.getProtocol() + ":" + jarPath.getPath() + "!/" + realName;
                                return new URL("jar", null, urlFile);
                            } catch (MalformedURLException e) {
                                throw new UncheckedIOException(e);
                            }
                        }

                        @Override
                        public byte[] getData() {
                            try {
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
                                            if (!closed) {
                                                throw new ZipFileMayHaveChangedException(e);
                                            }
                                            throw new RuntimeException("Unable to read " + name, e);
                                        }
                                    }
                                });
                            } catch (ZipFileMayHaveChangedException e) {
                                //this is a weird corner case, that should not really affect end users, but is super annoying
                                //if you are actually working on Quarkus. If you rebuild quarkus while you have an application
                                //running reading from the rebuilt zip file will fail, but some of these classes are needed
                                //for a clean shutdown, so the Java process hangs and needs to be forcibly killed
                                //this effectively attempts to reopen the file, allowing shutdown to work.
                                //we need to do this here as close needs a write lock while withJarFile takes a readLock
                                try {
                                    log.error("Failed to read " + name
                                            + " attempting to re-open the zip file. It is likely a jar file changed on disk, you should shutdown your application",
                                            e);
                                    close();
                                    return getData();
                                } catch (IOException ignore) {
                                    throw new RuntimeException("Unable to read " + name, e.getCause());
                                }
                            }
                        }

                        @Override
                        public boolean isDirectory() {
                            return res.getName().endsWith("/");
                        }
                    };
                }
                return null;

            }
        });
    }

    private <T> T withJarFile(Function<JarFile, T> func) {
        readLock.lock();
        try {
            if (closed) {
                //we still need this to work if it is closed, so shutdown hooks work
                //once it is closed it simply does not hold on to any resources
                try (JarFile jarFile = JarFiles.create(file)) {
                    return func.apply(jarFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return func.apply(jarFile);
            }
        } finally {
            readLock.unlock();
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
                    if (entry.getName().endsWith("/")) {
                        paths.add(entry.getName().substring(0, entry.getName().length() - 1));
                    } else {
                        paths.add(entry.getName());
                    }
                }
                //multi release jars can add additional entries
                if (JarFiles.isMultiRelease(jarFile)) {
                    String[] copyToIterate = paths.toArray(new String[0]);
                    for (String i : copyToIterate) {
                        if (i.startsWith(META_INF_VERSIONS)) {
                            String part = i.substring(META_INF_VERSIONS.length());
                            int slash = part.indexOf("/");
                            if (slash != -1) {
                                try {
                                    int ver = Integer.parseInt(part.substring(0, slash));
                                    if (ver <= JAVA_VERSION) {
                                        paths.add(part.substring(slash + 1));
                                    }
                                } catch (NumberFormatException e) {
                                    log.debug("Failed to parse META-INF/versions entry", e);
                                }
                            }
                        }
                    }
                }
                return paths;
            }
        }));
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        final URL url;
        try {
            url = jarPath.toURI().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + jarPath, e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        return new ProtectionDomain(codesource, null, classLoader, null);
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
        writeLock.lock();
        try {
            jarFile.close();
            closed = true;
        } finally {
            writeLock.unlock();
        }
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

    static class ZipFileMayHaveChangedException extends RuntimeException {
        public ZipFileMayHaveChangedException(Throwable cause) {
            super(cause);
        }
    }
}
