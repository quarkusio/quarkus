package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapGradleException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.devmode.DependenciesFilter;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ProcessedSources;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.dev.DevModeContext.ModuleInfo;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;

public class IDEDevModeMain implements BiConsumer<CuratedApplication, Map<String, Object>>, Closeable {

    private static final Logger log = Logger.getLogger(IDEDevModeMain.class.getName());
    private static final String APP_PROJECT = "app-project";

    private IsolatedDevModeMain delegate;

    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> stringObjectMap) {
        Path appClasses = (Path) stringObjectMap.get("app-classes");
        DevModeContext devModeContext = new DevModeContext();
        devModeContext.setArgs((String[]) stringObjectMap.get("args"));

        ApplicationModel appModel = null;
        try {
            if (BuildToolHelper.isMavenProject(appClasses)) {
                appModel = curatedApplication.getApplicationModel();
            } else {
                appModel = BootstrapUtils
                        .deserializeQuarkusModel((Path) stringObjectMap.get(BootstrapConstants.SERIALIZED_APP_MODEL));
            }

            if (appModel != null) {
                for (WorkspaceModule project : DependenciesFilter.getReloadableModules(appModel)) {
                    final ModuleInfo module = toModule(project);
                    if (project == appModel.getApplicationModule()) {
                        devModeContext.setApplicationRoot(module);
                    } else {
                        devModeContext.getAdditionalModules().add(module);
                        devModeContext.getLocalArtifacts()
                                .add(new AppArtifactKey(project.getId().getGroupId(), project.getId().getArtifactId()));
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

        String classesDir = null;
        final Set<Path> sourceParents = new LinkedHashSet<>();
        final PathsCollection.Builder srcPaths = PathsCollection.builder();
        for (ProcessedSources src : module.getMainSources()) {
            sourceParents.add(src.getSourceDir().getParentFile().toPath());
            srcPaths.add(src.getSourceDir().toPath());
            if (classesDir == null) {
                classesDir = src.getDestinationDir().toString();
            }
        }

        String resourceDirectory = null;
        final PathsCollection.Builder resourcesPaths = PathsCollection.builder();
        for (ProcessedSources src : module.getMainResources()) {
            resourcesPaths.add(src.getSourceDir().toPath());
            if (resourceDirectory == null) {
                // Peek the first one as we assume that it is the primary
                resourceDirectory = src.getDestinationDir().toString();
            }
        }

        final ArtifactKey key = new GACT(module.getId().getGroupId(), module.getId().getArtifactId());
        return new DevModeContext.ModuleInfo.Builder()
                .setArtifactKey(key)
                .setName(module.getId().getArtifactId())
                .setProjectDirectory(module.getModuleDir().getPath())
                .setSourcePaths(srcPaths.build())
                .setClassesPath(classesDir)
                .setResourcePaths(resourcesPaths.build())
                .setResourcesOutputPath(resourceDirectory)
                .setSourceParents(PathsCollection.from(sourceParents))
                .setPreBuildOutputDir(module.getBuildDir().toPath().resolve("generated-sources").toAbsolutePath().toString())
                .setTargetDir(module.getBuildDir().toString()).build();
    }
}
