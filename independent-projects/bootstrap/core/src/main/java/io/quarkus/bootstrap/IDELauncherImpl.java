package io.quarkus.bootstrap;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.resolver.model.WorkspaceModule;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import java.nio.file.Path;
import java.util.Map;

/**
 * IDE entry point.
 *
 * This is launched from the core/launcher module. To avoid any shading issues core/launcher unpacks all its dependencies
 * into the jar file, then uses a custom class loader load them.
 *
 */
public class IDELauncherImpl {

    public static void launch(Path projectRoot, Map<String, Object> context) {

        CuratedApplication app = null;
        try {
            //todo : proper support for everything
            final QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setApplicationRoot(projectRoot)
                    .setBaseClassLoader(IDELauncherImpl.class.getClassLoader())
                    .setProjectRoot(projectRoot)
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.DEV);

            if (BuildToolHelper.isGradleProject(projectRoot)) {
                final QuarkusModel quarkusModel = BuildToolHelper.enableGradleAppModelForDevMode(projectRoot);
                context.put(QuarkusModelHelper.SERIALIZED_QUARKUS_MODEL,
                        QuarkusModelHelper.serializeQuarkusModel(quarkusModel));

                final WorkspaceModule launchingModule = quarkusModel.getWorkspace().getMainModule();
                Path launchingModulePath = QuarkusModelHelper.getClassPath(launchingModule);
                // Gradle uses a different output directory for classes, we override the one used by the IDE
                builder.setProjectRoot(launchingModulePath)
                        .setApplicationRoot(launchingModulePath);

                for (WorkspaceModule additionalModule : quarkusModel.getWorkspace().getAllModules()) {
                    if (!additionalModule.getArtifactCoords().equals(launchingModule.getArtifactCoords())) {
                        builder.addAdditionalApplicationArchive(new AdditionalDependency(
                                QuarkusModelHelper.toPathsCollection(additionalModule.getSourceSet().getSourceDirectories()),
                                true, false));
                        builder.addAdditionalApplicationArchive(new AdditionalDependency(
                                additionalModule.getSourceSet().getResourceDirectory().toPath(), true, false));
                    }
                }
            }

            app = builder.build().bootstrap();

            app.runInAugmentClassLoader("io.quarkus.deployment.dev.IDEDevModeMain", context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (app != null) {
                app.close();
            }
        }
    }

}
