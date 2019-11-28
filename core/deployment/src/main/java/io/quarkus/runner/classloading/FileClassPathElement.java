package io.quarkus.runner.classloading;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class FileClassPathElement implements ClassPathElement {

    private final Path root;

    public FileClassPathElement(Path root) {
        this.root = root;
    }

    @Override
    public ClassPathResource getResource(String name) {
        Path file = root.resolve(name);
        if (Files.exists(file)) {
            return new ClassPathResource() {
                @Override
                public ClassPathElement getContainingElement() {
                    return FileClassPathElement.this;
                }

                @Override
                public String getPath() {
                    return name;
                }

                @Override
                public URL getUrl() {
                    try {
                        return file.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public byte[] getData() {
                    try {
                        return Files.readAllBytes(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to read " + file, e);
                    }
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
}
