package io.quarkus.bootstrap.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuarkusEntryPoint {

    public static final String QUARKUS_APPLICATION_DAT = "quarkus/quarkus-application.dat";

    public static void main(String... args) throws Throwable {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
        Timing.staticInitStarted();
        doRun(args);
    }

    private static void doRun(Object args) throws IOException, ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        String path = QuarkusEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        Path appRoot = new File(decodedPath).toPath().getParent().getParent().getParent();
        SerializedApplication app = null;
        try (InputStream in = Files.newInputStream(appRoot.resolve(QUARKUS_APPLICATION_DAT))) {
            app = SerializedApplication.read(in, appRoot);
            Thread.currentThread().setContextClassLoader(app.getRunnerClassLoader());
            Class<?> mainClass = app.getRunnerClassLoader().loadClass(app.getMainClass());
            mainClass.getMethod("main", String[].class).invoke(null, args);
        } finally {
            if (app != null) {
                app.getRunnerClassLoader().close();
            }
        }
    }
}
