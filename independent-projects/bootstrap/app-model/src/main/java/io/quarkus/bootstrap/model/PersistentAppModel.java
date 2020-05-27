package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A representation of AppModel, that has been serialized to disk for an existing application.
 *
 * This needs a slightly different representation than AppModel
 */
public class PersistentAppModel implements Serializable {

    private final String baseName;
    private final SerializedDep appArtifact;

    private final List<SerializedDep> deploymentDeps = new ArrayList<>();
    private final List<SerializedDep> fullDeploymentDeps = new ArrayList<>();
    private final List<SerializedDep> runtimeDeps = new ArrayList<>();
    private final Set<AppArtifactKey> parentFirstArtifacts = new HashSet<>();
    private final Set<AppArtifactKey> lesserPriorityArtifacts = new HashSet<>();
    private final Set<AppArtifactKey> localProjectArtifacts = new HashSet<>();

    public PersistentAppModel(String baseName, Map<AppArtifactKey, List<String>> paths, AppModel appModel,
            String appArchivePath) {
        this.baseName = baseName;
        appArtifact = new SerializedDep(appModel.getAppArtifact(), paths);
        appArtifact.paths = Collections.singletonList(appArchivePath);
        for (AppDependency i : appModel.getDeploymentDependencies()) {
            deploymentDeps.add(new SerializedDep(i, paths));
        }
        for (AppDependency i : appModel.getFullDeploymentDeps()) {
            fullDeploymentDeps.add(new SerializedDep(i, paths));
        }
        for (AppDependency i : appModel.getUserDependencies()) {
            runtimeDeps.add(new SerializedDep(i, paths));
        }
        localProjectArtifacts.addAll(appModel.getLocalProjectArtifacts());
        parentFirstArtifacts.addAll(appModel.getParentFirstArtifacts());
        lesserPriorityArtifacts.addAll(appModel.getLesserPriorityArtifacts());
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
        for (AppArtifactKey i : lesserPriorityArtifacts) {
            model.addLesserPriorityArtifact(i);
        }
        for (AppArtifactKey i : localProjectArtifacts) {
            model.addLocalProjectArtifact(i);
        }
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
            this.paths = paths.get(dependency.getKey());
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
