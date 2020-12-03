package io.quarkus.launcher;

import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * IDE entry point.
 * <p>
 * This has a number of hacks to make it work, and is always going to be a bit fragile, as it is hard to make something
 * that will work 100% of the time as we just don't have enough information.
 * <p>
 * The launcher module has all its dependencies shaded, so it is effectively self contained. This allows deployment time
 * code to not leak into runtime code, as the launcher artifact is explicitly excluded from the production build via a
 * hard coded exclusion.
 */
public class QuarkusLauncher {

    public static void launch(String callingClass, String quarkusApplication, String... args) {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try {
            String classResource = callingClass.replace(".", "/") + ".class";
            URL resource = Thread.currentThread().getContextClassLoader().getResource(classResource);
            String path = resource.getPath();
            path = path.substring(0, path.length() - classResource.length());
            URL newResource = new URL(resource.getProtocol(), resource.getHost(), resource.getPort(), path);

            URI uri = newResource.toURI();
            Path appClasses;
            if ("jar".equals(uri.getScheme())) {
                JarURLConnection connection = (JarURLConnection) uri.toURL().openConnection();
                connection.setDefaultUseCaches(false);
                appClasses = Paths.get(connection.getJarFileURL().toURI());
            } else {
                appClasses = Paths.get(uri);
            }
            if (quarkusApplication != null) {
                System.setProperty("quarkus.package.main-class", quarkusApplication);
            }

            Map<String, Object> context = new HashMap<>();
            context.put("app-classes", appClasses);
            context.put("args", args);

            RuntimeLaunchClassLoader loader = new RuntimeLaunchClassLoader(QuarkusLauncher.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(loader);

            Class<?> launcher = loader.loadClass("io.quarkus.bootstrap.IDELauncherImpl");
            launcher.getDeclaredMethod("launch", Path.class, Map.class).invoke(null, appClasses, context);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.clearProperty(BootstrapConstants.SERIALIZED_APP_MODEL);
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

}
