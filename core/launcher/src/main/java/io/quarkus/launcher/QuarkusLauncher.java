package io.quarkus.launcher;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;

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

    public static void launch(String callingClass, String quarkusApplication, Consumer<Integer> exitHandler, String... args) {

        try {
            String classResource = callingClass.replace(".", "/") + ".class";
            URL resource = Thread.currentThread().getContextClassLoader().getResource(classResource);
            String path = resource.getPath();
            path = path.substring(0, path.length() - classResource.length());
            URL newResource = new URL(resource.getProtocol(), resource.getHost(), resource.getPort(), path);

            Path appClasses = Paths.get(newResource.toURI());
            if (quarkusApplication != null) {
                System.setProperty("quarkus.package.main-class", quarkusApplication);
            }

            Map<String, Object> context = new HashMap<>();
            context.put("app-classes", appClasses);
            context.put("args", args);

            //todo : proper support for everything
            CuratedApplication app = QuarkusBootstrap.builder(appClasses)
                    .setBaseClassLoader(QuarkusLauncher.class.getClassLoader())
                    .setProjectRoot(appClasses)
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.DEV)
                    .build().bootstrap();
            app.runInAugmentClassLoader("io.quarkus.deployment.dev.IDEDevModeMain", context);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
