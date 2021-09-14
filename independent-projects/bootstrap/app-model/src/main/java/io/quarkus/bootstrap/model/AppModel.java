package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * A representation of the Quarkus dependency model for a given application.
 *
 * Changes made to this class should also be reflected in {@link PersistentAppModel}
 *
 * @author Alexey Loubyansky
 */
public class AppModel implements Serializable {

    public static final String PARENT_FIRST_ARTIFACTS = "parent-first-artifacts";
    public static final String RUNNER_PARENT_FIRST_ARTIFACTS = "runner-parent-first-artifacts";
    public static final String EXCLUDED_ARTIFACTS = "excluded-artifacts";
    public static final String LESSER_PRIORITY_ARTIFACTS = "lesser-priority-artifacts";

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

    /**
     * These artifacts have effect on the RunnerClassLoader
     */
    private final Set<AppArtifactKey> runnerParentFirstArtifacts;

    private final Set<AppArtifactKey> lesserPriorityArtifacts;

    /**
     * Artifacts that are present in the local maven project.
     *
     * These may be used by dev mode to make decisions about the final packaging for mutable jars
     */
    private final Set<AppArtifactKey> localProjectArtifacts;

    private final PlatformImports platformImports;

    private final Map<String, CapabilityContract> capabilitiesContracts;

    private AppModel(Builder builder) {
        this.appArtifact = builder.appArtifact;
        this.runtimeDeps = builder.filter(builder.runtimeDeps);
        this.deploymentDeps = builder.filter(builder.deploymentDeps);
        this.fullDeploymentDeps = builder.filter(builder.fullDeploymentDeps);
        this.parentFirstArtifacts = builder.parentFirstArtifacts;
        this.runnerParentFirstArtifacts = builder.runnerParentFirstArtifacts;
        this.lesserPriorityArtifacts = builder.lesserPriorityArtifacts;
        this.localProjectArtifacts = builder.localProjectArtifacts;
        this.platformImports = builder.platformImports;
        this.capabilitiesContracts = builder.capabilitiesContracts;
        log.debugf("Created AppModel %s", this);
    }

    public Map<String, String> getPlatformProperties() {
        return platformImports == null ? Collections.emptyMap() : platformImports.getPlatformProperties();
    }

    public PlatformImports getPlatforms() {
        return platformImports;
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

    public Set<AppArtifactKey> getRunnerParentFirstArtifacts() {
        return runnerParentFirstArtifacts;
    }

    public Set<AppArtifactKey> getLesserPriorityArtifacts() {
        return lesserPriorityArtifacts;
    }

    public Set<AppArtifactKey> getLocalProjectArtifacts() {
        return localProjectArtifacts;
    }

    public Map<String, CapabilityContract> getCapabilityContracts() {
        return capabilitiesContracts;
    }

    @Override
    public String toString() {
        return "AppModel{" +
                "appArtifact=" + appArtifact +
                ", deploymentDeps=" + deploymentDeps +
                ", fullDeploymentDeps=" + fullDeploymentDeps +
                ", runtimeDeps=" + runtimeDeps +
                ", parentFirstArtifacts=" + parentFirstArtifacts +
                ", runnerParentFirstArtifacts=" + runnerParentFirstArtifacts +
                '}';
    }

    public static class Builder {

        private AppArtifact appArtifact;

        private final List<AppDependency> deploymentDeps = new ArrayList<>();
        private final List<AppDependency> fullDeploymentDeps = new ArrayList<>();
        private final List<AppDependency> runtimeDeps = new ArrayList<>();
        private final Set<AppArtifactKey> parentFirstArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> runnerParentFirstArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> excludedArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> lesserPriorityArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> localProjectArtifacts = new HashSet<>();
        private PlatformImports platformImports;
        private Map<String, CapabilityContract> capabilitiesContracts = Collections.emptyMap();

        private Predicate<AppDependency> depPredicate;

        public Builder setAppArtifact(AppArtifact appArtifact) {
            this.appArtifact = appArtifact;
            return this;
        }

        public Builder setPlatformImports(PlatformImports platformImports) {
            this.platformImports = platformImports;
            return this;
        }

        public Builder setCapabilitiesContracts(Map<String, CapabilityContract> capabilitiesContracts) {
            this.capabilitiesContracts = capabilitiesContracts;
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

        public Builder addRunnerParentFirstArtifact(AppArtifactKey deps) {
            this.runnerParentFirstArtifacts.add(deps);
            return this;
        }

        public Builder addRunnerParentFirstArtifacts(List<AppArtifactKey> deps) {
            this.runnerParentFirstArtifacts.addAll(deps);
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

        public Builder addLesserPriorityArtifact(AppArtifactKey deps) {
            this.lesserPriorityArtifacts.add(deps);
            return this;
        }

        public Builder addLocalProjectArtifact(AppArtifactKey deps) {
            this.localProjectArtifacts.add(deps);
            return this;
        }

        public Builder addLocalProjectArtifacts(Collection<AppArtifactKey> deps) {
            this.localProjectArtifacts.addAll(deps);
            return this;
        }

        public Builder addLesserPriorityArtifacts(List<AppArtifactKey> deps) {
            this.lesserPriorityArtifacts.addAll(deps);
            return this;
        }

        /**
         * Sets the parent first and excluded artifacts from a descriptor properties file
         *
         * @param props The quarkus-extension.properties file
         */
        public void handleExtensionProperties(Properties props, String extension) {
            String parentFirst = props.getProperty(PARENT_FIRST_ARTIFACTS);
            if (parentFirst != null) {
                String[] artifacts = parentFirst.split(",");
                for (String artifact : artifacts) {
                    parentFirstArtifacts.add(new AppArtifactKey(artifact.split(":")));
                }
            }
            String runnerParentFirst = props.getProperty(RUNNER_PARENT_FIRST_ARTIFACTS);
            if (runnerParentFirst != null) {
                String[] artifacts = runnerParentFirst.split(",");
                for (String artifact : artifacts) {
                    runnerParentFirstArtifacts.add(new AppArtifactKey(artifact.split(":")));
                }
            }
            String excluded = props.getProperty(EXCLUDED_ARTIFACTS);
            if (excluded != null) {
                String[] artifacts = excluded.split(",");
                for (String artifact : artifacts) {
                    excludedArtifacts.add(new AppArtifactKey(artifact.split(":")));
                    log.debugf("Extension %s is excluding %s", extension, artifact);
                }
            }
            String lesserPriority = props.getProperty(LESSER_PRIORITY_ARTIFACTS);
            if (lesserPriority != null) {
                String[] artifacts = lesserPriority.split(",");
                for (String artifact : artifacts) {
                    lesserPriorityArtifacts.add(new AppArtifactKey(artifact.split(":")));
                    log.debugf("Extension %s is making %s a lesser priority artifact", extension, artifact);
                }
            }
        }

        private Predicate<AppDependency> dependencyPredicate() {
            if (depPredicate == null) {
                depPredicate = s -> {
                    // we never include the ide launcher in the final app model
                    if (s.getArtifact().getGroupId().equals("io.quarkus")
                            && s.getArtifact().getArtifactId().equals("quarkus-ide-launcher")) {
                        return false;
                    }
                    return !excludedArtifacts.contains(s.getArtifact().getKey());
                };
            }
            return depPredicate;
        }

        private List<AppDependency> filter(List<AppDependency> deps) {
            return deps.stream().filter(dependencyPredicate()).collect(Collectors.toList());
        }

        public AppModel build() {
            return new AppModel(this);
        }
    }
}
