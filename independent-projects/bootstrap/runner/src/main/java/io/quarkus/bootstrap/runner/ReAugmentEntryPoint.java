package io.quarkus.bootstrap.runner;

import java.io.File;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Experimental class that allows Quarkus applications to be re-augmented
 *
 */
public class ReAugmentEntryPoint {

    public static void main(String... args) throws Exception {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
        Timing.staticInitStarted();

        String path = ReAugmentEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        Path appRoot = new File(decodedPath).toPath().getParent().getParent();

        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(appRoot.resolve("deployment-quarkus/deployment-class-path.dat")))) {
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
