package io.quarkus.bootstrap.runneraot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.logging.InitialConfigurator;

/**
 * Entry point for AOT-optimized jar packaging.
 *
 * This entry point sets up a thin ClassLoader layer that caches frequently-accessed
 * resources (service files, configuration) while delegating all class loading to
 * standard Java classloaders for optimal AOT (Ahead-of-Time) compilation performance.
 */
public class AotEntryPoint {

    public static final String AOT_RESOURCE_CACHE_DAT = "quarkus/aot-resource-cache.dat";
    private static final String QUARKUS_DIR = "quarkus";

    public static void main(String... args) throws Throwable {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());

        try {
            doRun(args);
        } catch (RuntimeException | Error e) {
            InitialConfigurator.DELAYED_HANDLER.close();
            throw e;
        } catch (Throwable t) {
            InitialConfigurator.DELAYED_HANDLER.close();
            throw t;
        }
    }

    private static void doRun(String... args) throws Throwable {
        // Determine the application root directory
        // For aot-jar, the structure is:
        //   app-root/
        //     runner.jar (with Main-Class pointing here)
        //     lib/
        //       *.jar (including bootstrap-runner with AotEntryPoint)
        //     quarkus/
        //       aot-resource-cache.dat
        // Since AotEntryPoint might be loaded from lib/, we need to find the actual app root
        Path appRoot = determineAppRoot();

        // Load cached resources and main class name
        AotSerializedCache cache;
        Path cacheFile = appRoot.resolve(AOT_RESOURCE_CACHE_DAT);
        if (Files.exists(cacheFile)) {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(cacheFile), 8192)) {
                cache = AotSerializedCache.read(in);
            }
        } else {
            throw new IllegalStateException("AOT cache file not found: " + cacheFile
                    + ". The application may not have been packaged correctly with aot-jar type.");
        }

        String mainClassName = cache.getMainClass();
        Map<String, byte[]> cachedResources = cache.getCachedResources();

        // Build the classpath URLs for the classloader
        List<URL> urls = new ArrayList<>();

        // Add all jars from the app root directory (typically the runner jar)
        try (var stream = Files.list(appRoot)) {
            stream.filter(p -> p.toString().endsWith(".jar"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            urls.add(p.toUri().toURL());
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to add jar to classpath: " + p, e);
                        }
                    });
        }

        // Add lib directory jars
        Path libDir = appRoot.resolve("lib");
        if (Files.exists(libDir) && Files.isDirectory(libDir)) {
            try (var stream = Files.list(libDir)) {
                stream.filter(p -> p.toString().endsWith(".jar"))
                        .sorted()
                        .forEach(p -> {
                            try {
                                urls.add(p.toUri().toURL());
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to add jar to classpath: " + p, e);
                            }
                        });
            }
        }

        // Create the AOT classloader with cached resources
        AotClassLoader aotClassLoader = new AotClassLoader(
                urls.toArray(new URL[0]),
                ClassLoader.getSystemClassLoader(),
                cachedResources);

        try {
            // Set as context classloader
            Thread.currentThread().setContextClassLoader(aotClassLoader);

            // Load the main class and invoke its main method
            Class<?> mainClass = aotClassLoader.loadClass(mainClassName);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        } finally {
            aotClassLoader.close();
        }
    }

    /**
     * Determines the application root directory by finding the directory containing the 'quarkus' subdirectory.
     * This handles the case where AotEntryPoint might be loaded from a jar in the lib/ directory.
     */
    private static Path determineAppRoot() throws IOException {
        // Start with the location of this class
        String path = AotEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        Path currentPath = new File(decodedPath).toPath();

        // If it's a file (jar), start from its parent directory
        if (Files.isRegularFile(currentPath)) {
            currentPath = currentPath.getParent();
        }

        // Walk up the directory tree to find the directory containing 'quarkus/'
        Path candidate = currentPath;
        while (candidate != null) {
            Path quarkusDir = candidate.resolve(QUARKUS_DIR);
            if (Files.exists(quarkusDir) && Files.isDirectory(quarkusDir)) {
                return candidate;
            }
            candidate = candidate.getParent();
        }

        // Fallback: if we can't find quarkus/, assume current path's parent is the app root
        // This handles the case where AotEntryPoint is in lib/ and we need to go up one level
        if (Files.isDirectory(currentPath) && currentPath.getFileName().toString().equals("lib")) {
            return currentPath.getParent();
        }

        throw new IllegalStateException("Could not determine application root directory. "
                + "Started from: " + currentPath + ". "
                + "Please ensure the application is packaged correctly with aot-jar type.");
    }
}
