package io.quarkus.bootstrap.workspace;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

public class DefaultWorkspaceModule implements WorkspaceModule, Serializable {

    private static final long serialVersionUID = 6906903256002107806L;

    public static Builder builder() {
        return new DefaultWorkspaceModule().new Builder();
    }

    public class Builder implements WorkspaceModule.Mutable, Serializable {

        private Builder() {
        }

        @Override
        public Builder setModuleId(WorkspaceModuleId moduleId) {
            DefaultWorkspaceModule.this.id = moduleId;
            return this;
        }

        @Override
        public Builder setModuleDir(Path moduleDir) {
            DefaultWorkspaceModule.this.moduleDir = moduleDir.toFile();
            return this;
        }

        @Override
        public Builder setBuildDir(Path buildDir) {
            DefaultWorkspaceModule.this.buildDir = buildDir.toFile();
            return this;
        }

        @Override
        public Builder setBuildFile(Path buildFile) {
            DefaultWorkspaceModule.this.buildFiles = PathList.of(buildFile);
            return this;
        }

        @Override
        public Builder addDependencyConstraint(Dependency constraint) {
            if (DefaultWorkspaceModule.this.directDepConstraints == null) {
                DefaultWorkspaceModule.this.directDepConstraints = new ArrayList<>();
            }
            DefaultWorkspaceModule.this.directDepConstraints.add(constraint);
            return this;
        }

        @Override
        public Builder setDependencyConstraints(List<Dependency> constraints) {
            DefaultWorkspaceModule.this.directDepConstraints = constraints;
            return this;
        }

        @Override
        public Builder addDependency(Dependency dep) {
            if (DefaultWorkspaceModule.this.directDeps == null) {
                DefaultWorkspaceModule.this.directDeps = new ArrayList<>();
            }
            DefaultWorkspaceModule.this.directDeps.add(dep);
            return this;
        }

        @Override
        public Builder setDependencies(List<Dependency> deps) {
            DefaultWorkspaceModule.this.directDeps = deps;
            return this;
        }

        @Override
        public Builder addArtifactSources(ArtifactSources sources) {
            DefaultWorkspaceModule.this.sourcesSets.put(sources.getClassifier(), sources);
            return this;
        }

        @Override
        public boolean hasMainSources() {
            return DefaultWorkspaceModule.this.sourcesSets.containsKey(ArtifactSources.MAIN);
        }

        @Override
        public boolean hasTestSources() {
            return DefaultWorkspaceModule.this.sourcesSets.containsKey(ArtifactSources.TEST);
        }

        @Override
        public Builder setTestClasspathDependencyExclusions(Collection<String> excludes) {
            DefaultWorkspaceModule.this.testClasspathDependencyExclusions = excludes;
            return this;
        }

        @Override
        public Builder setAdditionalTestClasspathElements(Collection<String> elements) {
            DefaultWorkspaceModule.this.additionalTestClasspathElements = elements;
            return this;
        }

        @Override
        public Builder setParent(WorkspaceModule parent) {
            DefaultWorkspaceModule.this.parent = parent;
            return this;
        }

        @Override
        public WorkspaceModule build() {
            final DefaultWorkspaceModule module = DefaultWorkspaceModule.this;
            if (module.id == null) {
                throw new IllegalArgumentException("Module id is missing");
            }
            module.directDepConstraints = module.directDepConstraints == null ? Collections.emptyList()
                    : Collections.unmodifiableList(module.directDepConstraints);
            module.directDeps = module.directDeps == null ? Collections.emptyList()
                    : Collections.unmodifiableList(module.directDeps);
            module.sourcesSets = Collections.unmodifiableMap(module.sourcesSets);
            return module;
        }

        @Override
        public WorkspaceModuleId getId() {
            return DefaultWorkspaceModule.this.getId();
        }

        @Override
        public File getModuleDir() {
            return DefaultWorkspaceModule.this.getModuleDir();
        }

        @Override
        public File getBuildDir() {
            return DefaultWorkspaceModule.this.getBuildDir();
        }

        @Override
        public Collection<String> getSourceClassifiers() {
            return DefaultWorkspaceModule.this.getSourceClassifiers();
        }

        @Override
        public boolean hasSources(String classifier) {
            return DefaultWorkspaceModule.this.hasSources(classifier);
        }

        @Override
        public ArtifactSources getSources(String classifier) {
            return DefaultWorkspaceModule.this.getSources(classifier);
        }

        @Override
        public PathCollection getBuildFiles() {
            return DefaultWorkspaceModule.this.getBuildFiles();
        }

        @Override
        public Collection<Dependency> getDirectDependencyConstraints() {
            return DefaultWorkspaceModule.this.getDirectDependencyConstraints();
        }

        @Override
        public Collection<Dependency> getDirectDependencies() {
            return DefaultWorkspaceModule.this.getDirectDependencies();
        }

        @Override
        public Collection<String> getTestClasspathDependencyExclusions() {
            return DefaultWorkspaceModule.this.testClasspathDependencyExclusions;
        }

        @Override
        public Collection<String> getAdditionalTestClasspathElements() {
            return DefaultWorkspaceModule.this.additionalTestClasspathElements;
        }

        @Override
        public WorkspaceModule getParent() {
            return parent;
        }
    }

    private WorkspaceModuleId id;
    private File moduleDir;
    private File buildDir;
    private PathCollection buildFiles;
    private Map<String, ArtifactSources> sourcesSets = new HashMap<>();
    private List<Dependency> directDepConstraints;
    private List<Dependency> directDeps;

    /*
     * NOTE: we can't use List.of() methods because Gradle will fail to deserialize them.
     * See "Configuration cache error with Java11 collections #26942" https://github.com/gradle/gradle/issues/26942
     */
    private Collection<String> testClasspathDependencyExclusions = Collections.emptyList();
    private Collection<String> additionalTestClasspathElements = Collections.emptyList();
    private WorkspaceModule parent;

    private DefaultWorkspaceModule() {
    }

    private DefaultWorkspaceModule(WorkspaceModule module) {
        id = module.getId();
        moduleDir = module.getModuleDir();
        buildDir = module.getBuildDir();
        buildFiles = module.getBuildFiles().isEmpty() ? null : module.getBuildFiles();
        for (String classifier : module.getSourceClassifiers()) {
            sourcesSets.put(classifier, module.getSources(classifier));
        }
        directDepConstraints = module.getDirectDependencyConstraints().isEmpty() ? null
                : new ArrayList<>(module.getDirectDependencyConstraints());
        directDeps = module.getDirectDependencies().isEmpty() ? null : new ArrayList<>(module.getDirectDependencies());
    }

    public DefaultWorkspaceModule(WorkspaceModuleId id, File moduleDir, File buildDir) {
        super();
        this.id = id;
        this.moduleDir = moduleDir;
        this.buildDir = buildDir;
    }

    @Override
    public WorkspaceModuleId getId() {
        return id;
    }

    @Override
    public File getModuleDir() {
        return moduleDir;
    }

    @Override
    public File getBuildDir() {
        return buildDir;
    }

    @Override
    public boolean hasSources(String classifier) {
        return sourcesSets.containsKey(classifier);
    }

    @Override
    public ArtifactSources getSources(String name) {
        return sourcesSets.get(name);
    }

    @Override
    public Collection<String> getSourceClassifiers() {
        return sourcesSets.keySet();
    }

    public void setBuildFiles(PathCollection buildFiles) {
        this.buildFiles = buildFiles;
    }

    @Override
    public PathCollection getBuildFiles() {
        return buildFiles == null ? PathList.empty() : buildFiles;
    }

    @Override
    public Collection<Dependency> getDirectDependencyConstraints() {
        return directDepConstraints == null ? Collections.emptyList() : directDepConstraints;
    }

    @Override
    public Collection<Dependency> getDirectDependencies() {
        return directDeps == null ? Collections.emptyList() : directDeps;
    }

    @Override
    public Collection<String> getTestClasspathDependencyExclusions() {
        return testClasspathDependencyExclusions;
    }

    @Override
    public Collection<String> getAdditionalTestClasspathElements() {
        return additionalTestClasspathElements;
    }

    @Override
    public WorkspaceModule getParent() {
        return parent;
    }

    @Override
    public Mutable mutable() {
        return new DefaultWorkspaceModule(this).new Builder();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(id);
        buf.append(" ").append(moduleDir);
        sourcesSets.values().forEach(a -> {
            buf.append(" ").append(a);
        });
        return buf.toString();
    }
}
