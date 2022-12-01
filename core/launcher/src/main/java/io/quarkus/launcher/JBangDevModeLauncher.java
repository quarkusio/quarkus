package io.quarkus.launcher;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * JBang dev mode entry point. Utilises the same shaded info as the IDE launcher, but JBang has a different
 * launch path.
 *
 * Unlike the IDE launch use case in this case we have been able to store some info from the build phase,
 * so we don't need as many tricks.
 *
 * The launcher module has all its dependencies shaded, so it is effectively self-contained. This allows deployment time
 * code to not leak into runtime code, as the launcher artifact is explicitly excluded from the production build via a
 * hard coded exclusion.
 */
public class JBangDevModeLauncher {

    public static void main(String... args) {
        System.clearProperty("quarkus.dev"); //avoid unknown config key warnings
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try {

            Map<String, Object> context = new HashMap<>();
            context.put("args", args);
            RuntimeLaunchClassLoader loader = new RuntimeLaunchClassLoader(JBangDevModeLauncher.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(loader);

            Class<?> launcher = loader.loadClass("io.quarkus.bootstrap.jbang.JBangDevModeLauncherImpl");
            launcher.getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.clearProperty(BootstrapConstants.SERIALIZED_APP_MODEL);
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }
}
