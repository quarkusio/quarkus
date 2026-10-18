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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final Set<Path> removedFiles = new HashSet<>();

    public static void scheduleDelete(Collection<Path> deletedPaths) {
        synchronized (removedFiles) {
            for (Path deletedPath : deletedPaths) {
                if (removedFiles.add(deletedPath)) {
                    LOGGER.info("Scheduled for removal " + deletedPath);
                }
            }
        }
    }

    static void doDevMode(Path appRoot) throws IOException, ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        Path deploymentClassPath = appRoot.resolve(QuarkusEntryPoint.LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT);
        Closeable closeable = doStart(appRoot, deploymentClassPath);
        Timer timer = new Timer("Classpath Change Timer", false);
        timer.schedule(new ChangeDetector(appRoot, appRoot.resolve(QuarkusEntryPoint.LIB_DEPLOYMENT_APPMODEL_DAT),
                deploymentClassPath, closeable), 1000, 1000);

    }

    private static Closeable doStart(Path appRoot, Path deploymentClassPath) throws IOException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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
            return new AppProcessCleanup(
                    (Closeable) loader.loadClass("io.quarkus.deployment.mutability.DevModeTask")
                            .getDeclaredMethod("main", Path.class).invoke(null, appRoot),
                    loader);
        }
    }

    private static class AppProcessCleanup implements Closeable {

        private final Closeable app;
        private final URLClassLoader baseCl;

        public AppProcessCleanup(Closeable app, URLClassLoader baseCl) {
            this.app = app;
            this.baseCl = baseCl;
        }

        @Override
        public void close() throws IOException {
            app.close();
            baseCl.close();
        }
    }

    private static class ChangeDetector extends TimerTask {

        private static long getLastModified(Path appModelDat) throws IOException {
            return Files.getLastModifiedTime(appModelDat).toMillis();
        }

        private final Path appRoot;
        private final Path deploymentClassPath;

        /**
         * If a pom.xml file is changed then this file will be updated. So we just check it for changes.
         *
         * We use the {@link QuarkusEntryPoint#LIB_DEPLOYMENT_APPMODEL_DAT} instead of the
         * {@link QuarkusEntryPoint#LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT}
         * because the application model contains more information about the dependencies.
         * A mutable-jar will typically be built as a production application, which may be missing information about reloadable
         * dependencies.
         * When a mutable-jar is launched in remote-dev mode, the client will update the appmodel.dat with the information about
         * the reloadable dependencies.
         *
         * TODO: is there a potential issue with rsync based implementations not being fully synced? We can just sync this file
         * last but it gets tricky if we can't control the sync
         */
        private final Path appModelDat;
        private long lastModified;

        private Closeable closeable;

        public ChangeDetector(Path appRoot, Path appModelDat, Path deploymentClassPath, Closeable closeable)
                throws IOException {
            this.appRoot = appRoot;
            this.deploymentClassPath = deploymentClassPath;
            this.closeable = closeable;
            this.appModelDat = appModelDat;
            lastModified = getLastModified(appModelDat);
        }

        @Override
        public void run() {
            try {
                long time = getLastModified(appModelDat);
                if (lastModified != time) {
                    lastModified = time;
                    if (closeable != null) {
                        closeable.close();
                    }
                    synchronized (removedFiles) {
                        var removedFilesIterator = removedFiles.iterator();
                        while (removedFilesIterator.hasNext()) {
                            final Path removedFile = removedFilesIterator.next();
                            removedFilesIterator.remove();
                            var sb = new StringBuilder().append("Deleting ").append(removedFile);
                            if (!Files.deleteIfExists(removedFile)) {
                                sb.append(" didn't succeed");
                            }
                            LOGGER.info(sb.toString());
                        }
                    }
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
