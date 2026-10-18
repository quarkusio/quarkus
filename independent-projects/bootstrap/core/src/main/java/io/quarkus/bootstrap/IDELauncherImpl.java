package io.quarkus.bootstrap;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;

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
        System.setProperty("quarkus.console.basic", "true"); //IDE's don't support raw mode
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
                context.put(BootstrapConstants.SERIALIZED_APP_MODEL,
                        ApplicationModelSerializer.serializeGradleModel(quarkusModel, false));

                ArtifactSources mainSources = quarkusModel.getApplicationModule().getMainSources();

                PathsCollection applicationRoots = collectOutputDirs(mainSources);
                final Path launchingModulePath = applicationRoots.iterator().next();

                // Gradle uses a different output directory for classes, we override the one used by the IDE
                builder.setProjectRoot(launchingModulePath)
                        .setApplicationRoot(applicationRoots)
                        .setTargetDirectory(quarkusModel.getApplicationModule().getBuildDir().toPath());

                for (ResolvedDependency dep : quarkusModel.getDependencies()) {
                    final WorkspaceModule module = dep.getWorkspaceModule();
                    if (module != null) {
                        addAdditionalArchives(module.getSources(dep.getClassifier()), builder);
                    }
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

    private static PathsCollection collectOutputDirs(ArtifactSources mainSources) {
        PathsCollection.Builder outputDirs = PathsCollection.builder();
        collectOutputDirs(mainSources.getSourceDirs(), outputDirs);
        collectOutputDirs(mainSources.getResourceDirs(), outputDirs);
        return outputDirs.build();
    }

    private static void collectOutputDirs(Collection<SourceDir> sourceDirs, PathsCollection.Builder applicationRoots) {
        for (SourceDir sourceDir : sourceDirs) {
            if (!applicationRoots.contains(sourceDir.getOutputDir()) && Files.exists(sourceDir.getOutputDir())) {
                applicationRoots.add(sourceDir.getOutputDir());
            }
        }
    }

    private static void addAdditionalArchives(ArtifactSources sources, QuarkusBootstrap.Builder builder) {
        addAdditionalArchives(builder, sources.getSourceDirs());
        addAdditionalArchives(builder, sources.getResourceDirs());
    }

    private static void addAdditionalArchives(QuarkusBootstrap.Builder builder, Collection<SourceDir> sourceDirs) {
        for (SourceDir dir : sourceDirs) {
            if (Files.exists(dir.getOutputDir())) {
                builder.addAdditionalApplicationArchive(new AdditionalDependency(dir.getOutputDir(), true, false));
            }
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
