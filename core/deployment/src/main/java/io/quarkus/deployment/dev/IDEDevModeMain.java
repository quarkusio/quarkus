package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapGradleException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.model.gradle.WorkspaceModule;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.PathsUtils;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.dev.spi.DevModeType;

public class IDEDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>>, Closeable {

    private static final Logger log = Logger.getLogger(IDEDevModeMain.class.getName());
    private static final String APP_PROJECT = "app-project";

    private IsolatedDevModeMain delegate;

    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> stringObjectMap) {
        Path appClasses = (Path) stringObjectMap.get("app-classes");
        DevModeContext devModeContext = new DevModeContext();
        devModeContext.setArgs((String[]) stringObjectMap.get("args"));
        try {
            if (BuildToolHelper.isMavenProject(appClasses)) {
                LocalProject project = (LocalProject) stringObjectMap.get(APP_PROJECT);
                if (project == null) {
                    project = LocalProject.loadWorkspace(appClasses);
                }

                DevModeContext.ModuleInfo root = toModule(project);
                devModeContext.setApplicationRoot(root);

                final LocalWorkspace workspace = project.getWorkspace();
                for (AppArtifactKey localKey : curatedApplication.getAppModel().getLocalProjectArtifacts()) {
                    final LocalProject depProject = workspace.getProject(localKey.getGroupId(), localKey.getArtifactId());
                    if (project == depProject) {
                        continue;
                    }
                    if (depProject == null) {
                        throw new IllegalStateException(
                                "Failed to locate project dependency " + localKey + " in the workspace");
                    }
                    devModeContext.getAdditionalModules().add(toModule(depProject));
                    devModeContext.getLocalArtifacts().add(localKey);
                }
            } else {
                final QuarkusModel model = QuarkusModelHelper
                        .deserializeQuarkusModel((Path) stringObjectMap.get(QuarkusModelHelper.SERIALIZED_QUARKUS_MODEL));
                final WorkspaceModule launchingModule = model.getWorkspace().getMainModule();
                DevModeContext.ModuleInfo root = toModule(launchingModule);
                devModeContext.setApplicationRoot(root);
                for (WorkspaceModule additionalModule : model.getWorkspace().getAllModules()) {
                    if (!additionalModule.getArtifactCoords().equals(launchingModule.getArtifactCoords())) {
                        devModeContext.getAdditionalModules().add(toModule(additionalModule));
                    }
                }

            }
        } catch (AppModelResolverException e) {
            log.error("Failed to load workspace, hot reload will not be available", e);
        }

        terminateIfRunning();
        delegate = new IsolatedDevModeMain();
        Map<String, Object> params = new HashMap<>();
        params.put(DevModeContext.class.getName(), devModeContext);
        params.put(DevModeType.class.getName(), DevModeType.LOCAL);
        delegate.accept(curatedApplication,
                params);
    }

    @Override
    public void close() {
        terminateIfRunning();
    }

    private void terminateIfRunning() {
        if (delegate != null) {
            delegate.close();
        }
    }

    private DevModeContext.ModuleInfo toModule(WorkspaceModule module) throws BootstrapGradleException {
        AppArtifactKey key = new AppArtifactKey(module.getArtifactCoords().getGroupId(),
                module.getArtifactCoords().getArtifactId(), module.getArtifactCoords().getClassifier());

        final Set<Path> sourceParents = new LinkedHashSet<>();
        for (File srcDir : module.getSourceSourceSet().getSourceDirectories()) {
            sourceParents.add(srcDir.getParentFile().toPath());
        }
        String resourceDirectory = null;
        if (!module.getSourceSet().getResourceDirectories().isEmpty()) {
            // Peek the first one as we assume that it is the primary
            resourceDirectory = module.getSourceSet().getResourceDirectories().iterator().next().toString();
        }

        return new DevModeContext.ModuleInfo.Builder()
                .setAppArtifactKey(key)
                .setName(module.getArtifactCoords().getArtifactId())
                .setProjectDirectory(module.getProjectRoot().getPath())
                .setSourcePaths(PathsUtils.toPathsCollection(module.getSourceSourceSet().getSourceDirectories()))
                .setClassesPath(QuarkusModelHelper.getClassPath(module).toAbsolutePath().toString())
                .setResourcePaths(PathsUtils.toPathsCollection(module.getSourceSourceSet().getResourceDirectories()))
                .setResourcesOutputPath(resourceDirectory)
                .setSourceParents(PathsCollection.from(sourceParents))
                .setPreBuildOutputDir(module.getBuildDir().toPath().resolve("generated-sources").toAbsolutePath().toString())
                .setTargetDir(module.getBuildDir().toString()).build();
    }

    private DevModeContext.ModuleInfo toModule(LocalProject project) {
        return new DevModeContext.ModuleInfo.Builder()
                .setAppArtifactKey(project.getKey())
                .setName(project.getArtifactId())
                .setProjectDirectory(project.getDir().toAbsolutePath().toString())
                .setSourcePaths(PathsCollection.of(project.getSourcesSourcesDir().toAbsolutePath()))
                .setClassesPath(project.getClassesDir().toAbsolutePath().toString())
                .setResourcesOutputPath(project.getClassesDir().toAbsolutePath().toString())
                .setResourcePaths(
                        PathsCollection.from(project.getResourcesSourcesDirs().toList().stream()
                                .map(Path::toAbsolutePath)
                                .collect(Collectors.toCollection(LinkedHashSet::new))))
                .setSourceParents(PathsCollection.of(project.getSourcesDir()))
                .setPreBuildOutputDir(project.getCodeGenOutputDir().toString())
                .setTargetDir(project.getOutputDir().toString())
                .setTestSourcePaths(PathsCollection.of(project.getTestSourcesSourcesDir()))
                .setTestClassesPath(project.getTestClassesDir().toAbsolutePath().toString())
                .setTestResourcesOutputPath(project.getTestClassesDir().toAbsolutePath().toString())
                .setTestResourcePaths(PathsCollection.from(project.getTestResourcesSourcesDirs())).build();
    }
}
