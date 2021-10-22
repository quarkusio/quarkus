package io.quarkus.bootstrap.runner;

import io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThread;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class QuarkusEntryPoint {

    public static final String QUARKUS_APPLICATION_DAT = "quarkus/quarkus-application.dat";
    public static final String LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT = "lib/deployment/deployment-class-path.dat";

    public static void main(String... args) throws Throwable {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
        System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory",
                "io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory");
        Timing.staticInitStarted(false);
        doRun(args);
    }

    private static void doRun(Object args) throws IOException, ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        String path = QuarkusEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        Path appRoot = new File(decodedPath).toPath().getParent().getParent().getParent();

        if (Boolean.parseBoolean(System.getenv("QUARKUS_LAUNCH_DEVMODE"))) {
            DevModeMediator.doDevMode(appRoot);
        } else if (Boolean.getBoolean("quarkus.launch.rebuild")) {
            doReaugment(appRoot);
        } else {
            SerializedApplication app;
            // the magic number here is close to the smallest possible dat file
            try (InputStream in = new BufferedInputStream(Files.newInputStream(appRoot.resolve(QUARKUS_APPLICATION_DAT)),
                    24_576)) {
                app = SerializedApplication.read(in, appRoot);
            }
            final RunnerClassLoader appRunnerClassLoader = app.getRunnerClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(appRunnerClassLoader);
                QuarkusForkJoinWorkerThread.setQuarkusAppClassloader(appRunnerClassLoader);
                Class<?> mainClass = appRunnerClassLoader.loadClass(app.getMainClass());
                mainClass.getMethod("main", String[].class).invoke(null, args);
            } finally {
                QuarkusForkJoinWorkerThread.setQuarkusAppClassloader(null);
                appRunnerClassLoader.close();
            }
        }
    }

    private static void doReaugment(Path appRoot) throws IOException, ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(appRoot.resolve(LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT)))) {
            List<String> paths = (List<String>) in.readObject();
            //yuck, should use runner class loader
            URLClassLoader loader = new URLClassLoader(paths.stream().map((s) -> {
                try {
                    return appRoot.resolve(s).toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new));
            try {
                loader.loadClass("io.quarkus.deployment.mutability.ReaugmentTask")
                        .getDeclaredMethod("main", Path.class).invoke(null, appRoot);
            } finally {
                loader.close();
            }
        }
    }

}
