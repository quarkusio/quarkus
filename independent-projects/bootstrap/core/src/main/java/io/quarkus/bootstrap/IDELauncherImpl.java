package io.quarkus.bootstrap;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.devmode.DependenciesFilter;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.model.gradle.WorkspaceModule;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.PathsUtils;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import io.quarkus.bootstrap.utils.BuildToolHelper;
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

    public static final String LAUNCHED_FROM_IDE = "io.quarkus.launched-from-ide";

    public static Closeable launch(Path classesDir, Map<String, Object> context) {
        System.setProperty(LAUNCHED_FROM_IDE, "true");
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
                final QuarkusModel quarkusModel = BuildToolHelper.enableGradleAppModelForDevMode(classesDir);
                context.put(QuarkusModelHelper.SERIALIZED_QUARKUS_MODEL,
                        QuarkusModelHelper.serializeQuarkusModel(quarkusModel));

                final WorkspaceModule launchingModule = quarkusModel.getWorkspace().getMainModule();
                Path launchingModulePath = QuarkusModelHelper.getClassPath(launchingModule);
                // Gradle uses a different output directory for classes, we override the one used by the IDE
                builder.setProjectRoot(launchingModulePath)
                        .setApplicationRoot(launchingModulePath)
                        .setTargetDirectory(launchingModule.getBuildDir().toPath());

                for (WorkspaceModule additionalModule : quarkusModel.getWorkspace().getAllModules()) {
                    if (!additionalModule.getArtifactCoords().equals(launchingModule.getArtifactCoords())) {
                        builder.addAdditionalApplicationArchive(new AdditionalDependency(
                                PathsUtils.toPathsCollection(additionalModule.getSourceSet().getSourceDirectories()),
                                true, false));
                        builder.addAdditionalApplicationArchive(new AdditionalDependency(
                                PathsUtils.toPathsCollection(additionalModule.getSourceSet().getResourceDirectories()),
                                true, false));
                    }
                }
            } else {
                builder.setApplicationRoot(classesDir)
                        .setProjectRoot(projectDir);

                final BootstrapMavenContext mvnCtx = new BootstrapMavenContext(
                        BootstrapMavenContext.config().setCurrentProject(projectDir.toString()));

                final LocalProject currentProject = mvnCtx.getCurrentProject();
                context.put("app-project", currentProject);

                final MavenArtifactResolver mvnResolver = new MavenArtifactResolver(mvnCtx);
                builder.setMavenArtifactResolver(mvnResolver);

                DependenciesFilter.filterNotReloadableDependencies(currentProject, mvnResolver)
                        .forEach(p -> builder
                                .addLocalArtifact(new AppArtifactKey(p.getGroupId(), p.getArtifactId(), null, "jar")));
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
