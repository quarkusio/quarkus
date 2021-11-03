package io.quarkus.bootstrap.model;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A representation of AppModel, that has been serialized to disk for an existing application.
 * <p>
 * This needs a slightly different representation than AppModel
 */
public class MutableJarApplicationModel implements Serializable {

    private final String baseName;
    private final SerializedDep appArtifact;

    private List<SerializedDep> dependencies;
    private Set<ArtifactKey> parentFirstArtifacts;
    private Set<ArtifactKey> runnerParentFirstArtifacts;
    private Set<ArtifactKey> lesserPriorityArtifacts;
    private Set<ArtifactKey> localProjectArtifacts;
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
        parentFirstArtifacts = new HashSet<>(appModel.getParentFirst());
        runnerParentFirstArtifacts = new HashSet<>(appModel.getRunnerParentFirst());
        lesserPriorityArtifacts = new HashSet<>(appModel.getLowerPriorityArtifacts());
        capabilitiesContracts = new ArrayList<>(appModel.getExtensionCapabilities());
        this.platformImports = appModel.getPlatforms();
    }

    public String getUserProvidersDirectory() {
        return userProvidersDirectory;
    }

    public ApplicationModel getAppModel(Path root) {
        final ApplicationModelBuilder model = new ApplicationModelBuilder();
        model.setAppArtifact(appArtifact.getDep(root));
        for (SerializedDep i : dependencies) {
            model.addDependency(i.getDep(root));
        }
        for (ArtifactKey i : parentFirstArtifacts) {
            model.addParentFirstArtifact(i);
        }
        for (ArtifactKey i : runnerParentFirstArtifacts) {
            model.addRunnerParentFirstArtifact(i);
        }
        for (ArtifactKey i : lesserPriorityArtifacts) {
            model.addLesserPriorityArtifact(i);
        }
        for (ArtifactKey i : localProjectArtifacts) {
            model.addReloadableWorkspaceModule(i);
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

        public ResolvedDependency getDep(Path root) {
            final PathList.Builder builder = PathList.builder();
            for (String i : paths) {
                builder.add(root.resolve(i));
            }
            final ResolvedDependency d = ResolvedDependencyBuilder.newInstance()
                    .setGroupId(getGroupId())
                    .setArtifactId(getArtifactId())
                    .setClassifier(getClassifier())
                    .setVersion(getVersion())
                    .setResolvedPaths(builder.build())
                    .setFlags(flags)
                    .build();
            return d;
        }
    }
}
