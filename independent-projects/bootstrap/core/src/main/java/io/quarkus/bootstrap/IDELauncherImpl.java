package io.quarkus.bootstrap;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * IDE entry point.
 * <p>
 * This is launched from the core/launcher module. To avoid any shading issues core/launcher unpacks all its dependencies
 * into the jar file, then uses a custom class loader load them.
 */
public class IDELauncherImpl implements Closeable {

    public static final String FORCE_COLOR_SUPPORT = "io.quarkus.force-color-support";

    public static Closeable launch(Path classesDir, Map<String, Object> context) {
        System.setProperty(FORCE_COLOR_SUPPORT, "true");
        System.setProperty("quarkus.test.basic-console", "true"); //IDE's don't support raw mode
        final Path projectDir = BuildToolHelper.getProjectDir(classesDir);
        if (projectDir == null) {
            throw new IllegalStateException("Failed to locate project dir for " + classesDir);
        }
        try {
            //todo : proper support for everything
            final QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setBaseClassLoader(IDELauncherImpl.class.getClassLoader())
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.DEV)
                    .setTargetDirectory(classesDir.getParent());
            if (BuildToolHelper.isGradleProject(classesDir)) {
                final ApplicationModel quarkusModel = BuildToolHelper.enableGradleAppModelForDevMode(classesDir);
                context.put(BootstrapConstants.SERIALIZED_APP_MODEL, BootstrapUtils.serializeAppModel(quarkusModel, false));

                final Path launchingModulePath = quarkusModel.getApplicationModule().getMainSources().iterator().next()
                        .getDestinationDir().toPath();

                // Gradle uses a different output directory for classes, we override the one used by the IDE
                builder.setProjectRoot(launchingModulePath)
                        .setApplicationRoot(launchingModulePath)
                        .setTargetDirectory(quarkusModel.getApplicationModule().getBuildDir().toPath());

                for (WorkspaceModule additionalModule : quarkusModel.getWorkspaceModules()) {
                    additionalModule.getMainSources().forEach(src -> {
                        builder.addAdditionalApplicationArchive(
                                new AdditionalDependency(src.getDestinationDir().toPath(), true, false));

                    });
                    additionalModule.getMainResources().forEach(src -> {
                        builder.addAdditionalApplicationArchive(
                                new AdditionalDependency(src.getDestinationDir().toPath(), true, false));

                    });
                }
            } else {
                builder.setApplicationRoot(classesDir)
                        .setProjectRoot(projectDir);

                final BootstrapMavenContext mvnCtx = new BootstrapMavenContext(
                        BootstrapMavenContext.config().setCurrentProject(projectDir.toString()));

                final MavenArtifactResolver mvnResolver = new MavenArtifactResolver(mvnCtx);
                builder.setMavenArtifactResolver(mvnResolver);
            }

            final CuratedApplication curatedApp = builder.build().bootstrap();
            final Object appInstance = curatedApp.runInAugmentClassLoader("io.quarkus.deployment.dev.IDEDevModeMain", context);
            return new IDELauncherImpl(curatedApp,
                    appInstance == null ? null : appInstance instanceof Closeable ? (Closeable) appInstance : null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final CuratedApplication curatedApp;
    private final Closeable runningApp;

    private IDELauncherImpl(CuratedApplication curatedApp, Closeable runningApp) {
        this.curatedApp = curatedApp;
        this.runningApp = runningApp;
    }

    @Override
    public void close() throws IOException {
        try {
            if (runningApp != null) {
                runningApp.close();
            }
        } finally {
            if (curatedApp != null) {
                curatedApp.close();
            }
        }
    }
}
