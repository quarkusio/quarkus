package io.quarkus.deployment.dev;

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
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.resolver.model.WorkspaceModule;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import io.quarkus.bootstrap.utils.BuildToolHelper;

@SuppressWarnings("unused")
public class IDEDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>> {

    private static final Logger log = Logger.getLogger(IDEDevModeMain.class.getName());

    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> stringObjectMap) {
        Path appClasses = (Path) stringObjectMap.get("app-classes");
        DevModeContext devModeContext = new DevModeContext();
        devModeContext.setArgs((String[]) stringObjectMap.get("args"));
        try {
            if (BuildToolHelper.isMavenProject(appClasses)) {
                LocalProject project = LocalProject.loadWorkspace(appClasses);
                DevModeContext.ModuleInfo root = toModule(project);
                devModeContext.setApplicationRoot(root);
                for (Map.Entry<AppArtifactKey, LocalProject> module : project.getWorkspace().getProjects().entrySet()) {
                    if (module.getKey().equals(project.getKey())) {
                        continue;
                    }
                    devModeContext.getAdditionalModules().add(toModule(module.getValue()));
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

        new IsolatedDevModeMain().accept(curatedApplication,
                Collections.singletonMap(DevModeContext.class.getName(), devModeContext));
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
        return new DevModeContext.ModuleInfo(key,
                module.getArtifactCoords().getArtifactId(),
                module.getProjectRoot().getPath(),
                sourceDirectories,
                QuarkusModelHelper.getClassPath(module).toAbsolutePath().toString(),
                module.getSourceSourceSet().getResourceDirectory().toString(),
                resourceDirectory,
                sourceParents,
                module.getBuildDir().toPath().resolve("generated-sources").toAbsolutePath().toString(),
                module.getBuildDir().toString());
    }

    private DevModeContext.ModuleInfo toModule(LocalProject project) {
        return new DevModeContext.ModuleInfo(project.getKey(), project.getArtifactId(),
                project.getDir().toAbsolutePath().toString(),
                Collections.singleton(project.getSourcesSourcesDir().toAbsolutePath().toString()),
                project.getClassesDir().toAbsolutePath().toString(),
                project.getResourcesSourcesDir().toAbsolutePath().toString(),
                project.getSourcesDir().toString(),
                project.getCodeGenOutputDir().toString(),
                project.getOutputDir().toString());
    }
}
