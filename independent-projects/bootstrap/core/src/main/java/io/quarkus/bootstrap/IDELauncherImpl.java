package io.quarkus.bootstrap;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import java.nio.file.Path;
import java.util.Map;

/**
 * IDE entry point.
 * 
 * This is launched from the core/launcher module. To avoid any shading issues core/launcher unpacks all its dependencies
 * into the jar file, then uses a custom class loader load them.
 * 
 */
@SuppressWarnings("unused")
public class IDELauncherImpl {

    public static void launch(Path projectRoot, Map<String, Object> context) {

        try {
            //todo : proper support for everything
            CuratedApplication app = QuarkusBootstrap.builder(projectRoot)
                    .setBaseClassLoader(IDELauncherImpl.class.getClassLoader())
                    .setProjectRoot(projectRoot)
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.DEV)
                    .build().bootstrap();
            app.runInAugmentClassLoader("io.quarkus.deployment.dev.IDEDevModeMain", context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
