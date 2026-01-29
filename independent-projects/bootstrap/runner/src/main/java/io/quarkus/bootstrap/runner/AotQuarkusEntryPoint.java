package io.quarkus.bootstrap.runner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThread;
import io.quarkus.bootstrap.logging.InitialConfigurator;

/**
 * Entry point for AOT (Ahead-of-Time)-optimized jar packaging.
 *
 * This entry point sets up a thin class loader layer that caches frequently-accessed
 * resources (service files) while delegating all class loading to
 * standard Java class loaders for optimal AOT compilation performance.
 */
public class AotQuarkusEntryPoint {

    public static final String QUARKUS_APPLICATION_DAT = QuarkusEntryPoint.QUARKUS_APPLICATION_DAT;

    public static void main(String... args) throws Throwable {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
        System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory",
                "io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory");
        Timing.staticInitStarted(false);

        try {
            doRun(args);
        } catch (RuntimeException | Error e) {
            InitialConfigurator.DELAYED_HANDLER.close();
            throw e;
        } catch (Throwable t) {
            InitialConfigurator.DELAYED_HANDLER.close();
            throw new UndeclaredThrowableException(t);
        }
    }

    private static void doRun(String... args) throws Throwable {
        String path = null;
        URL location = AotQuarkusEntryPoint.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            // account for https://bugs.openjdk.org/browse/JDK-8376576
            String className = AotQuarkusEntryPoint.class.getSimpleName() + ".class";
            URL resource = AotQuarkusEntryPoint.class.getResource(className);
            if (resource != null) {
                String fullPath = resource.toString();
                if (fullPath.startsWith("jar:")) {
                    path = fullPath.substring(9, fullPath.indexOf("!"));
                }
            }
        } else {
            path = location.getPath();
        }
        if (path == null) {
            throw new IllegalStateException("Unable to determine launch jar path");
        }
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        Path appRoot = new File(decodedPath).toPath().getParent().getParent().getParent();

        // Load cached resources and main class name
        AotSerializedApplication app;
        Path cacheFile = appRoot.resolve(QUARKUS_APPLICATION_DAT);
        if (Files.isReadable(cacheFile)) {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(cacheFile), 8192)) {
                app = AotSerializedApplication.read(in);
            }
        } else {
            throw new IllegalStateException("Serialized application file not found or not readable: " + cacheFile
                    + ". The application may not have been packaged correctly with aot-jar type.");
        }

        final AotRunnerClassLoader aotRunnerClassLoader = app.getRunnerClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(aotRunnerClassLoader);
            QuarkusForkJoinWorkerThread.setQuarkusAppClassloader(aotRunnerClassLoader);
            Class<?> mainClass = aotRunnerClassLoader.loadClass(app.getMainClass());
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            lookup.findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class)).invokeExact(args);
        } finally {
            QuarkusForkJoinWorkerThread.setQuarkusAppClassloader(null);
        }
    }
}
