package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
public class PersistentAppModel implements Serializable {

    private final String baseName;
    private final SerializedDep appArtifact;

    private List<SerializedDep> dependencies;
    private Set<AppArtifactKey> parentFirstArtifacts;
    private Set<AppArtifactKey> runnerParentFirstArtifacts;
    private Set<AppArtifactKey> lesserPriorityArtifacts;
    private Set<AppArtifactKey> localProjectArtifacts;
    private Map<String, String> platformProperties;
    private Map<String, CapabilityContract> capabilitiesContracts;
    private String userProvidersDirectory;

    public PersistentAppModel(String baseName, Map<AppArtifactKey, List<String>> paths, AppModel appModel,
            String userProvidersDirectory, String appArchivePath) {
        this.baseName = baseName;
        this.userProvidersDirectory = userProvidersDirectory;
        appArtifact = new SerializedDep(appModel.getAppArtifact(), paths, 0);
        appArtifact.paths = Collections.singletonList(appArchivePath.replace('\\', '/'));
        dependencies = new ArrayList<>(appModel.getFullDeploymentDeps().size());
        for (AppDependency i : appModel.getFullDeploymentDeps()) {
            dependencies.add(new SerializedDep(i, paths, i.getFlags()));
        }
        platformProperties = new HashMap<>(appModel.getPlatformProperties());
        localProjectArtifacts = new HashSet<>(appModel.getLocalProjectArtifacts());
        parentFirstArtifacts = new HashSet<>(appModel.getParentFirstArtifacts());
        runnerParentFirstArtifacts = new HashSet<>(appModel.getRunnerParentFirstArtifacts());
        lesserPriorityArtifacts = new HashSet<>(appModel.getLesserPriorityArtifacts());
        capabilitiesContracts = new HashMap<>(appModel.getCapabilityContracts());
    }

    public String getUserProvidersDirectory() {
        return userProvidersDirectory;
    }

    public AppModel getAppModel(Path root) {
        AppModel.Builder model = new AppModel.Builder();
        model.setAppArtifact(appArtifact.getDep(root).getArtifact());
        for (SerializedDep i : dependencies) {
            model.addDependency(i.getDep(root));
        }
        for (AppArtifactKey i : parentFirstArtifacts) {
            model.addParentFirstArtifact(i);
        }
        for (AppArtifactKey i : runnerParentFirstArtifacts) {
            model.addRunnerParentFirstArtifact(i);
        }
        for (AppArtifactKey i : lesserPriorityArtifacts) {
            model.addLesserPriorityArtifact(i);
        }
        for (AppArtifactKey i : localProjectArtifacts) {
            model.addLocalProjectArtifact(i);
        }
        model.setCapabilitiesContracts(capabilitiesContracts);
        final PlatformImportsImpl pi = new PlatformImportsImpl();
        pi.setPlatformProperties(platformProperties);
        model.setPlatformImports(pi);
        return model.build();
    }

    public String getBaseName() {
        return baseName;
    }

    private static class SerializedDep extends AppArtifactCoords {

        private List<String> paths;
        private final int flags;

        public SerializedDep(AppArtifact dependency, Map<AppArtifactKey, List<String>> paths, int flags) {
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

        public SerializedDep(AppDependency dependency, Map<AppArtifactKey, List<String>> paths, int flags) {
            this(dependency.getArtifact(), paths, flags);
        }

        public AppDependency getDep(Path root) {
            PathsCollection.Builder builder = PathsCollection.builder();
            for (String i : paths) {
                builder.add(root.resolve(i));
            }

            AppArtifact appArtifact = new AppArtifact(getGroupId(), getArtifactId(), getClassifier(), getType(), getVersion());
            appArtifact.setPaths(builder.build());
            return new AppDependency(appArtifact, "compile", flags); //we don't care about scope at this point
        }
    }
}
