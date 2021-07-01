package io.quarkus.bootstrap.classloading;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A class path element that represents a file on the file system
 */
public class DirectoryClassPathElement extends AbstractClassPathElement {

    private final Path root;

    public DirectoryClassPathElement(Path root) {
        assert root != null : "root is null";
        this.root = root.normalize();
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public ClassPathResource getResource(String name) {
        final Path file;
        try {
            file = root.resolve(name);
        } catch (InvalidPathException ipe) {
            // can't resolve the resource
            return null;
        }
        Path normal = file.normalize();
        String cn = name;
        if (File.separatorChar == '\\') {
            cn = cn.replace('/', '\\');
        }
        if (!normal.startsWith(file)) {
            //don't allow directory escapes
            return null;
        }
        if (normal.toString().equals(cn)) {
            //this means that name is absolute (windows only, as the / would have been removed on linux)
            //we don't allow absolute paths
            return null;
        }
        if (!normal.endsWith(Paths.get(cn)) && !cn.isEmpty()) {
            //make sure the case is correct
            //if the file on disk does not match the case of name return null
            return null;
        }

        if (Files.exists(file)) {
            return new ClassPathResource() {
                @Override
                public ClassPathElement getContainingElement() {
                    return DirectoryClassPathElement.this;
                }

                @Override
                public String getPath() {
                    return name;
                }

                @Override
                public URL getUrl() {
                    try {
                        URI uri = file.toUri();
                        // the URLClassLoader doesn't add trailing slashes to directories, so we make sure we return
                        // the same URL as it would to avoid having QuarkusClassLoader return different URLs
                        // (one with a trailing slash and one without) for same resource
                        if (uri.getPath().endsWith("/")) {
                            String uriStr = uri.toString();
                            return new URL(uriStr.substring(0, uriStr.length() - 1));
                        }
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public byte[] getData() {
                    try {
                        try {
                            return Files.readAllBytes(file);
                        } catch (InterruptedIOException e) {
                            //if we are interrupted reading data we finish the op, then just re-interrupt the thread state
                            byte[] bytes = Files.readAllBytes(file);
                            Thread.currentThread().interrupt();
                            return bytes;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to read " + file, e);
                    }
                }

                @Override
                public boolean isDirectory() {
                    return Files.isDirectory(file);
                }
            };
        }
        return null;
    }

    @Override
    public Set<String> getProvidedResources() {
        try (Stream<Path> files = Files.walk(root)) {
            Set<String> paths = new HashSet<>();
            files.forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    if (!path.equals(root)) {
                        String st = root.relativize(path).toString();
                        if (!path.getFileSystem().getSeparator().equals("/")) {
                            st = st.replace(path.getFileSystem().getSeparator(), "/");
                        }
                        paths.add(st);
                    }
                }
            });
            return paths;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        URL url = null;
        try {
            URI uri = root.toUri();
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + root, e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, null, classLoader, null);
        return protectionDomain;
    }

    @Override
    public void close() throws IOException {
        //noop
    }

    @Override
    public String toString() {
        return root.toAbsolutePath().toString();
    }
}
