package io.quarkus.platform.descriptor.loader.json;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.io.FilenameUtils;

public final class ResourceLoaders {

    private static final String FILE = "file";
    private static final String JAR = "jar";

    private ResourceLoaders() {
    }

    public static File getResourceFile(final URL url, final String name) throws IOException {
        if (url == null) {
            throw new IOException("Failed to locate resource " + name + " on the classpath");
        }
        try {
            return new File(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new IOException(
                    "There were a problem while reading the resource dir '" + name + "' on the classpath with url: '"
                            + url.toString() + "'");
        }
    }

    public static ResourceLoader resolveFileResourceLoader(File f) {
        Objects.requireNonNull(f, "f is required");
        if (f.isDirectory()) {
            return new DirectoryResourceLoader(f.toPath());
        }
        if (f.isFile() && FilenameUtils.isExtension(f.getName(), "jar", "zip")) {
            return new ZipResourceLoader(f.toPath());
        }
        throw new IllegalStateException("No compatible ResourceLoader have been found for file type: " + f.getName());
    }

    // This method is copied from io.quarkus.runtime.util.ClassPathUtils
    public static <R> R processAsPath(URL url, Function<Path, R> function) {
        if (JAR.equals(url.getProtocol())) {
            final String file = url.getFile();
            final int exclam = file.lastIndexOf('!');
            final Path jar;
            try {
                jar = toLocalPath(exclam >= 0 ? new URL(file.substring(0, exclam)) : url);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to create a URL for '" + file.substring(0, exclam) + "'", e);
            }
            try (FileSystem jarFs = FileSystems.newFileSystem(jar, (ClassLoader) null)) {
                Path localPath = jarFs.getPath("/");
                if (exclam >= 0) {
                    localPath = localPath.resolve(file.substring(exclam + 1));
                }
                return function.apply(localPath);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read " + jar, e);
            }
        }

        if (FILE.equals(url.getProtocol())) {
            return function.apply(toLocalPath(url));
        }

        throw new IllegalArgumentException("Unexpected protocol " + url.getProtocol() + " for URL " + url);
    }

    private static Path toLocalPath(final URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to translate " + url + " to local path", e);
        }
    }
}
