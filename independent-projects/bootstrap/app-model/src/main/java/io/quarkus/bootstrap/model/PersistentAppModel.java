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

    private List<SerializedDep> deploymentDeps;
    private List<SerializedDep> fullDeploymentDeps;
    private List<SerializedDep> runtimeDeps;
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
        appArtifact = new SerializedDep(appModel.getAppArtifact(), paths);
        appArtifact.paths = Collections.singletonList(appArchivePath.replace("\\", "/"));
        deploymentDeps = new ArrayList<>(appModel.getDeploymentDependencies().size());
        for (AppDependency i : appModel.getDeploymentDependencies()) {
            deploymentDeps.add(new SerializedDep(i, paths));
        }
        fullDeploymentDeps = new ArrayList<>(appModel.getFullDeploymentDeps().size());
        for (AppDependency i : appModel.getFullDeploymentDeps()) {
            fullDeploymentDeps.add(new SerializedDep(i, paths));
        }
        runtimeDeps = new ArrayList<>(appModel.getUserDependencies().size());
        for (AppDependency i : appModel.getUserDependencies()) {
            runtimeDeps.add(new SerializedDep(i, paths));
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

        for (SerializedDep i : deploymentDeps) {
            model.addDeploymentDep(i.getDep(root));
        }
        for (SerializedDep i : fullDeploymentDeps) {
            model.addFullDeploymentDep(i.getDep(root));
        }
        for (SerializedDep i : runtimeDeps) {
            model.addRuntimeDep(i.getDep(root));
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

        public SerializedDep(AppArtifact dependency, Map<AppArtifactKey, List<String>> paths) {
            super(dependency.getGroupId(), dependency.getArtifactId(),
                    dependency.getClassifier(), dependency.getType(),
                    dependency.getVersion());
            List<String> pathList = paths.get(dependency.getKey());
            if (pathList == null) {
                pathList = Collections.emptyList();
            }
            this.paths = pathList.stream().map(s -> s.replace("\\", "/")).collect(Collectors.toList());
        }

        public SerializedDep(AppDependency dependency, Map<AppArtifactKey, List<String>> paths) {
            this(dependency.getArtifact(), paths);
        }

        public AppDependency getDep(Path root) {
            PathsCollection.Builder builder = PathsCollection.builder();
            for (String i : paths) {
                builder.add(root.resolve(i));
            }

            AppArtifact appArtifact = new AppArtifact(getGroupId(), getArtifactId(), getClassifier(), getType(), getVersion());
            appArtifact.setPaths(builder.build());
            return new AppDependency(appArtifact, "compile", false); //we don't care about scope at this point
        }

        public List<String> getPaths() {
            return paths;
        }
    }

}
