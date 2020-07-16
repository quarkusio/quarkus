package io.quarkus.deployment.dev;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;

@SuppressWarnings("unused")
public class IDEDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>> {

    private static final Logger log = Logger.getLogger(IDEDevModeMain.class.getName());

    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> stringObjectMap) {
        Path appClasses = (Path) stringObjectMap.get("app-classes");
        DevModeContext devModeContext = new DevModeContext();
        devModeContext.setArgs((String[]) stringObjectMap.get("args"));
        try {
            LocalProject project = LocalProject.loadWorkspace(appClasses);
            DevModeContext.ModuleInfo root = toModule(project);
            devModeContext.setApplicationRoot(root);
            for (Map.Entry<AppArtifactKey, LocalProject> module : project.getWorkspace().getProjects().entrySet()) {
                if (module.getKey().equals(project.getKey())) {
                    continue;
                }
                devModeContext.getAdditionalModules().add(toModule(module.getValue()));
            }
        } catch (BootstrapMavenException e) {
            log.error("Failed to load workspace, hot reload will not be available", e);
        }

        new IsolatedDevModeMain().accept(curatedApplication,
                Collections.singletonMap(DevModeContext.class.getName(), devModeContext));
    }

    private DevModeContext.ModuleInfo toModule(LocalProject project) {
        return new DevModeContext.ModuleInfo(project.getKey(), project.getArtifactId(),
                project.getDir().toAbsolutePath().toString(),
                Collections.singleton(project.getSourcesSourcesDir().toAbsolutePath().toString()),
                project.getClassesDir().toAbsolutePath().toString(),
                project.getResourcesSourcesDir().toAbsolutePath().toString(),
                project.getSourcesDir().toString(), project.getCodeGenOutputDir().toString(),
                project.getOutputDir().toString());
    }
}
