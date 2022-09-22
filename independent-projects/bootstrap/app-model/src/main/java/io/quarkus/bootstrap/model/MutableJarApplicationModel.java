package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

/**
 * A representation of AppModel, that has been serialized to disk for an existing application.
 * <p>
 * This needs a slightly different representation than AppModel
 */
public class MutableJarApplicationModel implements Serializable {

    private static final long serialVersionUID = 2046278141713688084L;

    private final String baseName;
    private final SerializedDep appArtifact;

    private List<SerializedDep> dependencies;
    private Set<ArtifactKey> localProjectArtifacts;
    private Map<ArtifactKey, Set<String>> excludedResources;
    private Collection<ExtensionCapabilities> capabilitiesContracts;
    private PlatformImports platformImports;
    private String userProvidersDirectory;

    public MutableJarApplicationModel(String baseName, Map<ArtifactKey, List<String>> paths, ApplicationModel appModel,
            String userProvidersDirectory, String appArchivePath) {
        this.baseName = baseName;
        this.userProvidersDirectory = userProvidersDirectory;
        appArtifact = new SerializedDep(appModel.getAppArtifact(), paths, 0);
        appArtifact.paths = Collections.singletonList(appArchivePath.replace('\\', '/'));
        dependencies = new ArrayList<>(appModel.getDependencies().size());
        for (ResolvedDependency i : appModel.getDependencies()) {
            dependencies.add(new SerializedDep(i, paths, i.getFlags()));
        }
        localProjectArtifacts = new HashSet<>(appModel.getReloadableWorkspaceDependencies());
        excludedResources = new HashMap<>(appModel.getRemovedResources());
        capabilitiesContracts = new ArrayList<>(appModel.getExtensionCapabilities());
        this.platformImports = appModel.getPlatforms();
    }

    public String getUserProvidersDirectory() {
        return userProvidersDirectory;
    }

    public ApplicationModel getAppModel(Path root) {
        final ApplicationModelBuilder model = new ApplicationModelBuilder();
        model.setAppArtifact(appArtifact.getDep(root).build());
        for (SerializedDep i : dependencies) {
            model.addDependency(i.getDep(root));
        }
        model.addReloadableWorkspaceModules(localProjectArtifacts);
        for (Map.Entry<ArtifactKey, Set<String>> i : excludedResources.entrySet()) {
            model.addRemovedResources(i.getKey(), i.getValue());
        }
        for (ExtensionCapabilities ec : capabilitiesContracts) {
            model.addExtensionCapabilities(ec);
        }
        model.setPlatformImports(platformImports);
        return model.build();
    }

    public String getBaseName() {
        return baseName;
    }

    private static class SerializedDep extends GACTV {

        private List<String> paths;
        private final int flags;

        public SerializedDep(ResolvedDependency dependency, Map<ArtifactKey, List<String>> paths, int flags) {
            super(dependency.getGroupId(), dependency.getArtifactId(),
                    dependency.getClassifier(), dependency.getType(),
                    dependency.getVersion());
            List<String> pathList = paths.get(dependency.getKey());
            if (pathList == null) {
                pathList = Collections.emptyList();
            }
            this.paths = pathList.stream().map(s -> s.replace('\\', '/')).collect(Collectors.toList());
            this.flags = flags;
        }

        public ResolvedDependencyBuilder getDep(Path root) {
            final PathList.Builder builder = PathList.builder();
            for (String i : paths) {
                builder.add(root.resolve(i));
            }
            return ResolvedDependencyBuilder.newInstance()
                    .setGroupId(getGroupId())
                    .setArtifactId(getArtifactId())
                    .setClassifier(getClassifier())
                    .setVersion(getVersion())
                    .setResolvedPaths(builder.build())
                    .setFlags(flags);
        }
    }
}
