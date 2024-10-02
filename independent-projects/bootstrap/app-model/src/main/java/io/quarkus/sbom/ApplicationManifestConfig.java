package io.quarkus.sbom;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactKey;

public class ApplicationManifestConfig {

    public static Builder builder() {
        return new ApplicationManifestConfig().new Builder();
    }

    public class Builder {

        private class ComponentHolder {
            private Path path;
            private ApplicationComponent component;

            private ComponentHolder(ApplicationComponent component) {
                this.component = component;
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
            setMainComponent(ApplicationComponent.builder().setResolvedDependency(model.getAppArtifact()).build());
            for (var d : model.getDependencies()) {
                final ApplicationComponent.Builder comp = ApplicationComponent.builder().setResolvedDependency(d);
                if (!d.getResolvedPaths().isEmpty()) {
                    comp.setPath(d.getResolvedPaths().iterator().next());
                }
                addComponent(comp.build());
            }
            return this;
        }

        public Builder setMainComponent(ApplicationComponent applicationRunner) {
            ensureNotBuilt();
            main = applicationRunner == null ? null : new ComponentHolder(applicationRunner);
            return this;
        }

        public Builder setDistributionDirectory(Path distributionDirectory) {
            ensureNotBuilt();
            distDir = distributionDirectory;
            return this;
        }

        public Builder setRunnerPath(Path runnerPath) {
            ensureNotBuilt();
            ApplicationManifestConfig.this.runnerPath = runnerPath;
            return this;
        }

        public Builder addComponent(ApplicationComponent component) {
            ComponentHolder holder = null;
            if (component.getPath() != null) {
                holder = compPaths.get(component.getPath());
            }
            if (holder == null && component.getResolvedDependency() != null) {
                holder = compArtifacts.get(component.getResolvedDependency().getKey());
            }
            if (holder == null) {
                holder = new ComponentHolder(component);
                if (holder.path != null) {
                    compPaths.put(holder.path, holder);
                }
                if (holder.component.getResolvedDependency() != null) {
                    compArtifacts.put(holder.component.getResolvedDependency().getKey(), holder);
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
                if (holder.component.getResolvedDependency() == null && component.getResolvedDependency() != null) {
                    holder.component = component;
                    compArtifacts.put(holder.component.getResolvedDependency().getKey(), holder);
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
            return this;
        }

        public ApplicationManifestConfig build() {
            ensureNotBuilt();
            Objects.requireNonNull(main, "mainComponent is null");
            built = true;
            if (!compList.isEmpty()) {
                ApplicationManifestConfig.this.components = new ArrayList<>(compList.size());
                for (var holder : compList) {
                    if (holder.path != null && !holder.path.equals(holder.component.getPath())) {
                        holder.component = ApplicationComponent.builder()
                                .setPath(holder.path)
                                .setResolvedDependency(holder.component.getResolvedDependency())
                                .setPedigree(holder.component.getPedigree());
                    }
                    if (distDir != null) {
                        setDistributionPath(holder, false);
                    }
                    ApplicationManifestConfig.this.components.add(holder.component.ensureImmutable());
                }
            }
            if (distDir != null) {
                setDistributionPath(main, true);
            }
            ApplicationManifestConfig.this.main = main.component.ensureImmutable();
            return ApplicationManifestConfig.this;
        }

        private void setDistributionPath(ComponentHolder holder, boolean main) {
            ApplicationComponent c = holder.component;
            if (c.getDistributionPath() == null
                    && c.getPath() != null && c.getPath().startsWith(distDir)) {
                final ApplicationComponent.Builder builder;
                if (c instanceof ApplicationComponent.Builder) {
                    builder = (ApplicationComponent.Builder) c;
                } else {
                    builder = new ApplicationComponent.Builder(c);
                    if (!main) {
                        holder.component = builder;
                    }
                }
                var relativePath = distDir.relativize(c.getPath());
                var sb = new StringBuilder();
                for (var i = 0; i < relativePath.getNameCount(); ++i) {
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

    private ApplicationManifestConfig() {
    }

    private Path distDir;
    private Path runnerPath;
    private ApplicationComponent main;
    private List<ApplicationComponent> components = List.of();

    public Path getDistributionDirectory() {
        return distDir;
    }

    public Path getRunnerPath() {
        return runnerPath;
    }

    public ApplicationComponent getMainComponent() {
        return main;
    }

    public Collection<ApplicationComponent> getComponents() {
        return components;
    }
}
