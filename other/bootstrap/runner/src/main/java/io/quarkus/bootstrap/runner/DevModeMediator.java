package io.quarkus.bootstrap.runner;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.jboss.logging.Logger;

/**
 * Starts dev mode from a mutable image
 *
 * Also will restart if the image has changed.
 */
public class DevModeMediator {

    protected static final Logger LOGGER = Logger.getLogger(DevModeMediator.class);

    static void doDevMode(Path appRoot) throws IOException, ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        Path deploymentClassPath = appRoot.resolve(QuarkusEntryPoint.LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT);
        Closeable closeable = doStart(appRoot, deploymentClassPath);
        Timer timer = new Timer("Classpath Change Timer", false);
        timer.schedule(new ChangeDetector(appRoot, deploymentClassPath, closeable), 1000, 1000);

    }

    private static Closeable doStart(Path appRoot, Path deploymentClassPath) throws IOException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Closeable closeable = null;
        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(deploymentClassPath))) {
            List<String> paths = (List<String>) in.readObject();
            //yuck, should use runner class loader
            URLClassLoader loader = new URLClassLoader(paths.stream().map((s) -> {
                try {
                    return appRoot.resolve(s).toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new));
            closeable = (Closeable) loader.loadClass("io.quarkus.deployment.mutability.DevModeTask")
                    .getDeclaredMethod("main", Path.class).invoke(null, appRoot);
        }
        return closeable;
    }

    private static class ChangeDetector extends TimerTask {
        private final Path appRoot;
        /**
         * If the pom.xml file is changed then this file will be updated
         *
         * So we just check it for changes.
         *
         * TODO: is there a potential issue with rsync based implementations not being fully synced? We can just sync this file
         * last
         * but it gets tricky if we can't control the sync
         */
        private final Path deploymentClassPath;

        private long lastModified;

        private Closeable closeable;

        public ChangeDetector(Path appRoot, Path deploymentClassPath, Closeable closeable) throws IOException {
            this.appRoot = appRoot;
            this.deploymentClassPath = deploymentClassPath;
            this.closeable = closeable;
            lastModified = Files.getLastModifiedTime(deploymentClassPath).toMillis();
        }

        @Override
        public void run() {

            try {
                long time = Files.getLastModifiedTime(deploymentClassPath).toMillis();
                if (lastModified != time) {
                    lastModified = time;
                    if (closeable != null) {
                        closeable.close();
                    }
                    closeable = null;
                    try {
                        closeable = doStart(appRoot, deploymentClassPath);
                    } catch (Exception e) {
                        LOGGER.error("Failed to restart app after classpath changes", e);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to check for classpath changes", e);
            }

        }

    }

}
