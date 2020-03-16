package io.quarkus.bootstrap.model;

import io.quarkus.bootstrap.BootstrapConstants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * A representation of the Quarkus dependency model for a given application.
 *
 * @author Alexey Loubyansky
 */
public class AppModel implements Serializable {

    private static final Logger log = Logger.getLogger(AppModel.class);

    private final AppArtifact appArtifact;

    /**
     * The deployment dependencies, less the runtime parts. This will likely go away
     */
    private final List<AppDependency> deploymentDeps;
    /**
     * The deployment dependencies, including all transitive dependencies. This is used to build an isolated class
     * loader to run the augmentation
     */
    private final List<AppDependency> fullDeploymentDeps;

    /**
     * The runtime dependencies of the application, including the runtime parts of all extensions.
     */
    private final List<AppDependency> runtimeDeps;

    private final Set<AppArtifactKey> parentFirstArtifacts;

    private final Set<AppArtifactKey> lesserPriorityArtifacts;

    private AppModel(AppArtifact appArtifact, List<AppDependency> runtimeDeps, List<AppDependency> deploymentDeps,
            List<AppDependency> fullDeploymentDeps, Set<AppArtifactKey> parentFirstArtifacts,
            Set<AppArtifactKey> lesserPriorityArtifacts) {
        this.appArtifact = appArtifact;
        this.runtimeDeps = runtimeDeps;
        this.deploymentDeps = deploymentDeps;
        this.fullDeploymentDeps = fullDeploymentDeps;
        this.parentFirstArtifacts = parentFirstArtifacts;
        this.lesserPriorityArtifacts = lesserPriorityArtifacts;
    }

    public AppArtifact getAppArtifact() {
        return appArtifact;
    }

    /**
     * Dependencies that the user has added that have nothing to do with Quarkus (3rd party libs, additional modules etc)
     */
    public List<AppDependency> getUserDependencies() {
        return runtimeDeps;
    }

    /**
     * Dependencies of the -deployment artifacts from the quarkus extensions, and all their transitive dependencies.
     *
     */
    @Deprecated
    public List<AppDependency> getDeploymentDependencies() {
        return deploymentDeps;
    }

    public List<AppDependency> getFullDeploymentDeps() {
        return fullDeploymentDeps;
    }

    public Set<AppArtifactKey> getParentFirstArtifacts() {
        return parentFirstArtifacts;
    }

    public Set<AppArtifactKey> getLesserPriorityArtifacts() {
        return lesserPriorityArtifacts;
    }

    @Override
    public String toString() {
        return "AppModel{" +
                "appArtifact=" + appArtifact +
                ", deploymentDeps=" + deploymentDeps +
                ", fullDeploymentDeps=" + fullDeploymentDeps +
                ", runtimeDeps=" + runtimeDeps +
                ", parentFirstArtifacts=" + parentFirstArtifacts +
                '}';
    }

    public static class Builder {

        private AppArtifact appArtifact;

        private final List<AppDependency> deploymentDeps = new ArrayList<>();
        private final List<AppDependency> fullDeploymentDeps = new ArrayList<>();
        private final List<AppDependency> runtimeDeps = new ArrayList<>();
        private final Set<AppArtifactKey> parentFirstArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> excludedArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> lesserPriorityArtifacts = new HashSet<>();

        public Builder setAppArtifact(AppArtifact appArtifact) {
            this.appArtifact = appArtifact;
            return this;
        }

        public Builder addDeploymentDep(AppDependency dep) {
            this.deploymentDeps.add(dep);
            return this;
        }

        public Builder addDeploymentDeps(List<AppDependency> deps) {
            this.deploymentDeps.addAll(deps);
            return this;
        }

        public Builder addFullDeploymentDep(AppDependency dep) {
            this.fullDeploymentDeps.add(dep);
            return this;
        }

        public Builder addFullDeploymentDeps(List<AppDependency> deps) {
            this.fullDeploymentDeps.addAll(deps);
            return this;
        }

        public Builder addRuntimeDep(AppDependency dep) {
            this.runtimeDeps.add(dep);
            return this;
        }

        public Builder addRuntimeDeps(List<AppDependency> deps) {
            this.runtimeDeps.addAll(deps);
            return this;
        }

        public Builder addParentFirstArtifact(AppArtifactKey deps) {
            this.parentFirstArtifacts.add(deps);
            return this;
        }

        public Builder addParentFirstArtifacts(List<AppArtifactKey> deps) {
            this.parentFirstArtifacts.addAll(deps);
            return this;
        }

        public Builder addExcludedArtifact(AppArtifactKey deps) {
            this.excludedArtifacts.add(deps);
            return this;
        }

        public Builder addExcludedArtifacts(List<AppArtifactKey> deps) {
            this.excludedArtifacts.addAll(deps);
            return this;
        }

        /**
         * Sets the parent first and excluded artifacts from a descriptor properties file
         *
         * @param props The quarkus-extension.properties file
         */
        public void handleExtensionProperties(Properties props, String extension) {
            String parentFirst = props.getProperty(BootstrapConstants.PARENT_FIRST_ARTIFACTS);
            if (parentFirst != null) {
                String[] artifacts = parentFirst.split(",");
                for (String artifact : artifacts) {
                    parentFirstArtifacts.add(new AppArtifactKey(artifact.split(":")));
                }
            }
            String excluded = props.getProperty(BootstrapConstants.EXCLUDED_ARTIFACTS);
            if (excluded != null) {
                String[] artifacts = excluded.split(",");
                for (String artifact : artifacts) {
                    excludedArtifacts.add(new AppArtifactKey(artifact.split(":")));
                    log.debugf("Extension %s is excluding %s", extension, artifact);
                }
            }
            String lesserPriority = props.getProperty(BootstrapConstants.LESSER_PRIORITY_ARTIFACTS);
            if (lesserPriority != null) {
                String[] artifacts = lesserPriority.split(",");
                for (String artifact : artifacts) {
                    lesserPriorityArtifacts.add(new AppArtifactKey(artifact.split(":")));
                    log.debugf("Extension %s is making %s a lesser priority artifact", extension, artifact);
                }
            }
        }

        public AppModel build() {
            Predicate<AppDependency> includePredicate = s -> {
                //we never include the ide launcher in the final app model
                if (s.getArtifact().getGroupId().equals("io.quarkus")
                        && s.getArtifact().getArtifactId().equals("quarkus-ide-launcher")) {
                    return false;
                }
                return !excludedArtifacts.contains(s.getArtifact().getKey());
            };
            List<AppDependency> runtimeDeps = this.runtimeDeps.stream().filter(includePredicate).collect(Collectors.toList());
            List<AppDependency> deploymentDeps = this.deploymentDeps.stream().filter(includePredicate)
                    .collect(Collectors.toList());
            List<AppDependency> fullDeploymentDeps = this.fullDeploymentDeps.stream().filter(includePredicate)
                    .collect(Collectors.toList());
            AppModel appModel = new AppModel(appArtifact, runtimeDeps, deploymentDeps, fullDeploymentDeps,
                    parentFirstArtifacts, lesserPriorityArtifacts);
            log.debugf("Created AppMode %s", appModel);
            return appModel;

        }
    }
}
