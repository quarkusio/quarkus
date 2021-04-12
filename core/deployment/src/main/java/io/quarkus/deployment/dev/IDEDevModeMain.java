package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapGradleException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.model.gradle.WorkspaceModule;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import io.quarkus.bootstrap.utils.BuildToolHelper;

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
        delegate.accept(curatedApplication,
                Collections.singletonMap(DevModeContext.class.getName(), devModeContext));
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

        Set<String> sourceDirectories = new HashSet<>();
        Set<String> sourceParents = new HashSet<>();
        for (File srcDir : module.getSourceSourceSet().getSourceDirectories()) {
            sourceDirectories.add(srcDir.getPath());
            sourceParents.add(srcDir.getParent());
        }
        String resourceDirectory = null;
        if (module.getSourceSet().getResourceDirectory() != null) {
            resourceDirectory = module.getSourceSet().getResourceDirectory().getPath();
        }
        return new DevModeContext.ModuleInfo.Builder()
                .setAppArtifactKey(key)
                .setName(module.getArtifactCoords().getArtifactId())
                .setProjectDirectory(module.getProjectRoot().getPath())
                .setSourcePaths(sourceDirectories)
                .setClassesPath(QuarkusModelHelper.getClassPath(module).toAbsolutePath().toString())
                .setResourcePath(module.getSourceSourceSet().getResourceDirectory().toString())
                .setResourcesOutputPath(resourceDirectory)
                .setSourceParents(sourceParents)
                .setPreBuildOutputDir(module.getBuildDir().toPath().resolve("generated-sources").toAbsolutePath().toString())
                .setTargetDir(module.getBuildDir().toString()).build();
    }

    private DevModeContext.ModuleInfo toModule(LocalProject project) {

        return new DevModeContext.ModuleInfo.Builder()
                .setAppArtifactKey(project.getKey())
                .setName(project.getArtifactId())
                .setProjectDirectory(project.getDir().toAbsolutePath().toString())
                .setSourcePaths(Collections.singleton(project.getSourcesSourcesDir().toAbsolutePath().toString()))
                .setClassesPath(project.getClassesDir().toAbsolutePath().toString())
                .setResourcesOutputPath(project.getClassesDir().toAbsolutePath().toString())
                .setResourcePath(project.getResourcesSourcesDir().toAbsolutePath().toString())
                .setSourceParents(Collections.singleton(project.getSourcesDir().toString()))
                .setPreBuildOutputDir(project.getCodeGenOutputDir().toString())
                .setTargetDir(project.getOutputDir().toString()).build();
    }
}