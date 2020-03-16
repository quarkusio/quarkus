package io.quarkus.runtime.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClassPathUtils {

    private static final String FILE = "file";
    private static final String JAR = "jar";

    /**
     * Invokes {@link #consumeAsStreams(ClassLoader, String, Consumer)} passing in
     * an instance of the current thread's context classloader as the classloader
     * from which to load the resources.
     *
     * @param resource resource path
     * @param consumer resource input stream consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsStreams(String resource, Consumer<InputStream> consumer) throws IOException {
        consumeAsStreams(Thread.currentThread().getContextClassLoader(), resource, consumer);
    }

    /**
     * Locates all the occurrences of a resource on the classpath of the provided classloader
     * and invokes the consumer providing the input streams for each located resource.
     * The consumer does not have to close the provided input stream.
     * This method was introduced to avoid calling {@link java.net.URL#openStream()} which
     * in case the resource is found in an archive (such as JAR) locks the containing archive
     * even if the caller properly closes the stream.
     *
     * @param cl classloader to load the resources from
     * @param resource resource path
     * @param consumer resource input stream consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsStreams(ClassLoader cl, String resource, Consumer<InputStream> consumer) throws IOException {
        final Enumeration<URL> resources = cl.getResources(resource);
        while (resources.hasMoreElements()) {
            consumeStream(resources.nextElement(), consumer);
        }
    }

    /**
     * Invokes {@link #consumeAsPaths(ClassLoader, String, Consumer)} passing in
     * an instance of the current thread's context classloader as the classloader
     * from which to load the resources.
     *
     * @param resource resource path
     * @param consumer resource path consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsPaths(String resource, Consumer<Path> consumer) throws IOException {
        consumeAsPaths(Thread.currentThread().getContextClassLoader(), resource, consumer);
    }

    /**
     * Locates specified resources on the classpath and attempts to represent them as local file system paths
     * to be processed by a consumer. If a resource appears to be an actual file or a directory, it is simply
     * passed to the consumer as-is. If a resource is an entry in a JAR, the entry will be resolved as an instance
     * of {@link java.nio.file.Path} in a {@link java.nio.file.FileSystem} representing the JAR.
     * If the protocol of the URL representing the resource is neither 'file' nor 'jar', the method will fail
     * with an exception.
     *
     * @param cl classloader to load the resources from
     * @param resource resource path
     * @param consumer resource path consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsPaths(ClassLoader cl, String resource, Consumer<Path> consumer) throws IOException {
        final Enumeration<URL> resources = cl.getResources(resource);
        while (resources.hasMoreElements()) {
            consumeAsPath(resources.nextElement(), consumer);
        }
    }

    /**
     * Attempts to represent a resource as a local file system path to be processed by a consumer.
     * If a resource appears to be an actual file or a directory, it is simply passed to the consumer as-is.
     * If a resource is an entry in a JAR, the entry will be resolved as an instance
     * of {@link java.nio.file.Path} in a {@link java.nio.file.FileSystem} representing the JAR.
     * If the protocol of the URL representing the resource is neither 'file' nor 'jar', the method will fail
     * with an exception.
     *
     * @param url resource url
     * @param consumer resource path consumer
     */
    public static void consumeAsPath(URL url, Consumer<Path> consumer) {
        processAsPath(url, p -> {
            consumer.accept(p);
            return null;
        });
    }

    /**
     * Attempts to represent a resource as a local file system path to be processed by a function.
     * If a resource appears to be an actual file or a directory, it is simply passed to the function as-is.
     * If a resource is an entry in a JAR, the entry will be resolved as an instance
     * of {@link java.nio.file.Path} in a {@link java.nio.file.FileSystem} representing the JAR.
     * If the protocol of the URL representing the resource is neither 'file' nor 'jar', the method will fail
     * with an exception.
     *
     * @param url resource url
     * @param function resource path function
     */
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

    /**
     * Invokes a consumer providing the input streams to read the content of the URL.
     * The consumer does not have to close the provided input stream.
     * This method was introduced to avoid calling {@link java.net.URL#openStream()} which
     * in case the resource is found in an archive (such as JAR) locks the containing archive
     * even if the caller properly closes the stream.
     *
     * @param url URL
     * @param consumer input stream consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeStream(URL url, Consumer<InputStream> consumer) throws IOException {
        readStream(url, is -> {
            consumer.accept(is);
            return null;
        });
    }

    /**
     * Invokes a function providing the input streams to read the content of the URL.
     * The function does not have to close the provided input stream.
     * This method was introduced to avoid calling {@link java.net.URL#openStream()} which
     * in case the resource is found in an archive (such as JAR) locks the containing archive
     * even if the caller properly closes the stream.
     *
     * @param url URL
     * @param function input stream processing function
     * @throws IOException in case of an IO failure
     */
    public static <R> R readStream(URL url, Function<InputStream, R> function) throws IOException {
        if (JAR.equals(url.getProtocol())) {
            final String file = url.getFile();
            final int exclam = file.lastIndexOf('!');
            final Path jar = toLocalPath(exclam >= 0 ? new URL(file.substring(0, exclam)) : url);
            try (FileSystem jarFs = FileSystems.newFileSystem(jar, (ClassLoader) null)) {
                try (InputStream is = Files.newInputStream(jarFs.getPath(file.substring(exclam + 1)))) {
                    return function.apply(is);
                }
            }
        }
        if (FILE.equals(url.getProtocol())) {
            try (InputStream is = Files.newInputStream(toLocalPath(url))) {
                return function.apply(is);
            }
        }
        try (InputStream is = url.openStream()) {
            return function.apply(is);
        }
    }

    /**
     * Translates a URL to local file system path.
     * In case the the URL couldn't be translated to a file system path,
     * an instance of {@link IllegalArgumentException} will be thrown.
     *
     * @param url URL
     * @return local file system path
     */
    public static Path toLocalPath(final URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to translate " + url + " to local path", e);
        }
    }
}
