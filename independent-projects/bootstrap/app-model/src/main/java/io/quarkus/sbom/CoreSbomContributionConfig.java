package io.quarkus.sbom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Assembles the core Quarkus application SBOM contribution from Maven artifacts
 * and packaged files.
 * <p>
 * This class collects {@link ComponentDescriptor}s from the application's dependency model
 * and jar packaging steps, deduplicates them by file path and Maven artifact key, resolves
 * distribution paths relative to the build output directory, and produces the resulting
 * {@link SbomContribution} including the main application component and dependency graph.
 * <p>
 * Only the core contribution designates the main application component and runner path.
 * Extension-contributed {@link SbomContribution}s should not provide a main component.
 * <p>
 * Typical usage:
 *
 * <pre>{@code
 * var config = CoreSbomContributionConfig.builder()
 *         .setApplicationModel(appModel)
 *         .setDistributionDirectory(buildDir)
 *         .setRunnerPath(runnerJar)
 *         .build();
 * SbomContribution contribution = config.toSbomContribution();
 * }</pre>
 */
public class CoreSbomContributionConfig {

    public static Builder builder() {
        return new CoreSbomContributionConfig().new Builder();
    }

    public class Builder {

        private class ComponentHolder {
            private Path path;
            private ComponentDescriptor component;
            private ResolvedDependency dep;
            private Collection<ArtifactCoords> explicitDependencies;

            private ComponentHolder(ComponentDescriptor component, ResolvedDependency dep) {
                this.component = component;
                this.dep = dep;
                this.path = component.getPath();
            }
        }

        private boolean built;
        private ComponentHolder main;
        private Map<ArtifactKey, ComponentHolder> compArtifacts = new HashMap<>();
        private Map<Path, ComponentHolder> compPaths = new HashMap<>();
        private List<ComponentHolder> compList = new ArrayList<>();

        private Builder() {
        }

        public Builder setApplicationModel(ApplicationModel model) {
            setMainComponent(toDescriptor(model.getAppArtifact()), model.getAppArtifact());
            return addComponents(model.getDependencies());
        }

        public Builder addComponents(Collection<ResolvedDependency> dependencies) {
            for (ResolvedDependency d : dependencies) {
                addComponent(toDescriptor(d), d);
            }
            return this;
        }

        private static ComponentDescriptor toDescriptor(ResolvedDependency d) {
            Purl purl = Purl.maven(d.getGroupId(), d.getArtifactId(), d.getVersion(),
                    d.getType(), d.getClassifier().isEmpty() ? null : d.getClassifier());
            ComponentDescriptor.Builder builder = ComponentDescriptor.builder()
                    .setPurl(purl)
                    .setScope(d.isRuntimeCp() ? ComponentDescriptor.SCOPE_RUNTIME : ComponentDescriptor.SCOPE_DEVELOPMENT);
            if (!d.getResolvedPaths().isEmpty()) {
                builder.setPath(d.getResolvedPaths().iterator().next());
            }
            return builder;
        }

        public Builder setMainComponent(ComponentDescriptor main) {
            return setMainComponent(main, null, null);
        }

        public Builder setMainComponent(ComponentDescriptor main, ResolvedDependency dep) {
            return setMainComponent(main, dep, null);
        }

        public Builder setMainComponent(ComponentDescriptor main, Collection<ArtifactCoords> dependencies) {
            return setMainComponent(main, null, dependencies);
        }

        private Builder setMainComponent(ComponentDescriptor main, ResolvedDependency dep,
                Collection<ArtifactCoords> dependencies) {
            ensureNotBuilt();
            this.main = main == null ? null : addComponentHolder(main, dep);
            if (this.main != null && dependencies != null) {
                this.main.explicitDependencies = dependencies;
            }
            return this;
        }

        public Builder setDistributionDirectory(Path distributionDirectory) {
            ensureNotBuilt();
            distDir = distributionDirectory;
            return this;
        }

        public Builder setRunnerPath(Path runnerPath) {
            ensureNotBuilt();
            CoreSbomContributionConfig.this.runnerPath = runnerPath;
            return this;
        }

        public Builder addComponent(ComponentDescriptor component) {
            return addComponent(component, null);
        }

        public Builder addComponent(ComponentDescriptor component, ResolvedDependency dep) {
            addComponentHolder(component, dep);
            return this;
        }

        private ComponentHolder addComponentHolder(ComponentDescriptor component, ResolvedDependency dep) {
            if (component.scope == null && dep != null) {
                component.scope = dep.isRuntimeCp()
                        ? ComponentDescriptor.SCOPE_RUNTIME
                        : ComponentDescriptor.SCOPE_DEVELOPMENT;
            }
            ComponentHolder holder = null;
            if (component.getPath() != null) {
                holder = compPaths.get(component.getPath());
            }
            if (holder == null && dep != null) {
                holder = compArtifacts.get(dep.getKey());
            }
            if (holder == null) {
                holder = new ComponentHolder(component, dep);
                if (holder.path != null) {
                    compPaths.put(holder.path, holder);
                }
                if (dep != null) {
                    compArtifacts.put(dep.getKey(), holder);
                }
                compList.add(holder);
            } else {
                if (component.getPath() != null) {
                    if (holder.path != null) {
                        compPaths.remove(holder.path);
                    }
                    holder.path = component.getPath();
                    compPaths.put(holder.path, holder);
                }
                if (holder.dep == null && dep != null) {
                    holder.component = component;
                    holder.dep = dep;
                    compArtifacts.put(dep.getKey(), holder);
                }
                if (component.getPedigree() != null) {
                    if (holder.component.getPedigree() == null) {
                        holder.component.pedigree = component.getPedigree();
                    } else if (!holder.component.getPedigree().contains(component.getPedigree())) {
                        holder.component.pedigree += System.lineSeparator() + component.getPedigree();
                    }
                }
                if (component.getScope() != null) {
                    holder.component.scope = component.scope;
                }
            }
            return holder;
        }

        public CoreSbomContributionConfig build() {
            ensureNotBuilt();
            Objects.requireNonNull(main, "mainComponent is null");
            built = true;
            final Set<String> directDepPurls = collectDirectDependencyPurls(main);
            if (!compList.isEmpty()) {
                CoreSbomContributionConfig.this.components = new ArrayList<>(compList.size());
                CoreSbomContributionConfig.this.dependencies = new ArrayList<>(compList.size());
                for (ComponentHolder holder : compList) {
                    if (holder == main) {
                        continue;
                    }
                    if (holder.path != null && !holder.path.equals(holder.component.getPath())) {
                        ComponentDescriptor.Builder builder = new ComponentDescriptor.Builder(holder.component);
                        builder.setPath(holder.path);
                        holder.component = builder;
                    }
                    if (distDir != null) {
                        setDistributionPath(holder, false);
                    }
                    defaultScope(holder);
                    if (isDirectDependency(holder, directDepPurls)) {
                        holder.component.topLevel = true;
                    }
                    CoreSbomContributionConfig.this.components.add(holder.component.ensureImmutable());
                    addDependencies(holder);
                }
            }
            if (distDir != null) {
                setDistributionPath(main, true);
            }
            defaultScope(main);
            CoreSbomContributionConfig.this.main = main.component.ensureImmutable();
            addDependencies(main);
            return CoreSbomContributionConfig.this;
        }

        private static void defaultScope(ComponentHolder holder) {
            if (holder.component.scope == null) {
                holder.component.scope = ComponentDescriptor.SCOPE_RUNTIME;
            }
        }

        /**
         * Collects the PURL strings of the main component's direct dependencies.
         */
        private static Set<String> collectDirectDependencyPurls(ComponentHolder main) {
            Collection<ArtifactCoords> deps = main.explicitDependencies;
            if (deps == null || deps.isEmpty()) {
                deps = main.dep != null ? main.dep.getDependencies() : null;
            }
            if (deps == null || deps.isEmpty()) {
                return Set.of();
            }
            Set<String> result = new HashSet<String>(deps.size());
            for (ArtifactCoords coord : deps) {
                result.add(mavenPurl(coord).toString());
            }
            return result;
        }

        /**
         * Checks whether the given component matches one of the main component's
         * direct dependencies.
         */
        private static boolean isDirectDependency(ComponentHolder holder, Set<String> directDepPurls) {
            return directDepPurls.contains(holder.component.getBomRef());
        }

        private void addDependencies(ComponentHolder holder) {
            Collection<ArtifactCoords> deps = holder.explicitDependencies;
            if (deps == null || deps.isEmpty()) {
                deps = holder.dep != null ? holder.dep.getDependencies() : null;
            }
            if (deps == null || deps.isEmpty()) {
                return;
            }
            List<String> depBomRefs = new ArrayList<String>(deps.size());
            for (ArtifactCoords depCoords : deps) {
                depBomRefs.add(mavenPurl(depCoords).toString());
            }
            CoreSbomContributionConfig.this.dependencies.add(
                    ComponentDependencies.of(holder.component.getBomRef(), depBomRefs));
        }

        private static Purl mavenPurl(ArtifactCoords coords) {
            return Purl.maven(coords.getGroupId(), coords.getArtifactId(), coords.getVersion(),
                    coords.getType(),
                    coords.getClassifier().isEmpty() ? null : coords.getClassifier());
        }

        private void setDistributionPath(ComponentHolder holder, boolean main) {
            ComponentDescriptor c = holder.component;
            if (c.getDistributionPath() == null
                    && c.getPath() != null && c.getPath().startsWith(distDir)) {
                final ComponentDescriptor.Builder builder;
                if (c instanceof ComponentDescriptor.Builder) {
                    builder = (ComponentDescriptor.Builder) c;
                } else {
                    builder = new ComponentDescriptor.Builder(c);
                    if (!main) {
                        holder.component = builder;
                    }
                }
                Path relativePath = distDir.relativize(c.getPath());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < relativePath.getNameCount(); ++i) {
                    if (i > 0) {
                        sb.append("/");
                    }
                    sb.append(relativePath.getName(i));
                }
                builder.setDistributionPath(sb.toString());
            }
        }

        private void ensureNotBuilt() {
            if (built) {
                throw new RuntimeException("This builder instance has already been built");
            }
        }
    }

    private CoreSbomContributionConfig() {
    }

    private Path distDir;
    private Path runnerPath;
    private ComponentDescriptor main;
    private List<ComponentDescriptor> components = List.of();
    private List<ComponentDependencies> dependencies = List.of();

    public Path getDistributionDirectory() {
        return distDir;
    }

    public Path getRunnerPath() {
        return runnerPath;
    }

    public ComponentDescriptor getMainComponent() {
        return main;
    }

    public Collection<ComponentDescriptor> getComponents() {
        return components;
    }

    public Collection<ComponentDependencies> getDependencies() {
        return dependencies;
    }

    /**
     * Converts this config into an {@link SbomContribution}, resolving distribution paths
     * and adding remaining distribution content if a distribution directory is set.
     * <p>
     * This is the only path that designates a main application component in the SBOM.
     *
     * @return an SbomContribution representing the core application manifest
     */
    public SbomContribution toSbomContribution() {
        CoreSbomContributionConfig config = this;
        final Collection<ComponentDependencies> originalDependencies = config.getDependencies();
        if (config.getDistributionDirectory() != null) {
            Builder builder = CoreSbomContributionConfig.builder()
                    .setDistributionDirectory(config.getDistributionDirectory())
                    .setMainComponent(config.getMainComponent())
                    .setRunnerPath(config.getRunnerPath());
            for (ComponentDescriptor c : config.getComponents()) {
                builder.addComponent(c);
            }
            addRemainingContent(config, builder);
            config = builder.build();
        }
        SbomContribution.Builder sb = SbomContribution.builder();
        sb.setMainComponentBomRef(config.getMainComponent().getBomRef());
        sb.setRunnerPath(config.getRunnerPath());
        sb.addComponent(config.getMainComponent());
        for (ComponentDescriptor c : config.getComponents()) {
            sb.addComponent(c);
        }
        for (ComponentDependencies d : originalDependencies) {
            sb.addDependency(d);
        }
        return sb.build();
    }

    private static void addRemainingContent(CoreSbomContributionConfig config, Builder builder) {
        try {
            Files.walkFileTree(config.getDistributionDirectory(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    builder.addComponent(ComponentDescriptor.builder()
                            .setPurl(Purl.generic(file.getFileName().toString(), config.getMainComponent().getVersion()))
                            .setPath(file));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
