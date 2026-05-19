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
import java.util.Set;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Assembles the core Quarkus application SBOM contribution from Maven artifacts
 * and packaged files.
 * <p>
 * This class collects raw inputs — the {@link ApplicationModel}, main component identity,
 * additional component descriptors, and file system paths — and defers all processing
 * until {@link #toSbomContribution()} is called. In particular, {@link ComponentDescriptor}s
 * for the application model's dependencies are not created eagerly during configuration.
 * <p>
 * The main application component is resolved in the following priority order:
 * <ol>
 * <li>If {@link #setMainPurl(Purl)} is set, a new component with that PURL is created
 * (typically a {@linkplain Purl#generic generic} PURL for native images or runner jars).</li>
 * <li>If {@link #setMainArtifact(ResolvedDependency)} is set, a Maven-PURL component
 * is derived from it.</li>
 * <li>Otherwise the application artifact from the {@link ApplicationModel} is used.</li>
 * </ol>
 * <p>
 * Only the core contribution designates a main application component. Extension-contributed
 * {@link SbomContribution}s should not provide one.
 * <p>
 * Typical usage:
 *
 * <pre>{@code
 * var contribution = new CoreSbomContributionConfig()
 *         .setApplicationModel(appModel)
 *         .setDistributionDirectory(buildDir)
 *         .setMainPath(runnerJar)
 *         .toSbomContribution();
 * }</pre>
 */
public class CoreSbomContributionConfig {

    private static class ComponentHolder {
        Path path;
        ComponentDescriptor component;
        ResolvedDependency dep;
        String pedigree;
        Collection<ArtifactCoords> explicitDependencies;

        ComponentHolder(ComponentDescriptor component) {
            this.component = component;
            this.path = component.getPath();
        }

        ComponentHolder(ResolvedDependency dep, Path path, String pedigree) {
            this.dep = dep;
            this.path = path;
            this.pedigree = pedigree;
        }
    }

    private ApplicationModel applicationModel;
    private Path distDir;
    private Path runnerPath;
    private Purl mainPurl;
    private ResolvedDependency mainArtifact;
    private Collection<ArtifactCoords> mainDependencies;
    private List<ComponentHolder> additionalComponents = new ArrayList<>();

    /**
     * Sets the application model whose dependencies will be converted to
     * {@link ComponentDescriptor}s when {@link #toSbomContribution()} is called.
     * The model's application artifact also serves as the default main component
     * when neither {@link #setMainPurl} nor {@link #setMainArtifact} is specified.
     *
     * @param model the resolved application model
     * @return this config
     */
    public CoreSbomContributionConfig setApplicationModel(ApplicationModel model) {
        this.applicationModel = model;
        return this;
    }

    /**
     * Sets the main component from a resolved Maven artifact. A Maven PURL and
     * dependency list are derived from it automatically. Use this when the main
     * component is a Maven artifact whose identity differs from the application
     * model's application artifact, or when no application model is set.
     *
     * @param dep the resolved dependency representing the main artifact
     * @return this config
     */
    public CoreSbomContributionConfig setMainArtifact(ResolvedDependency dep) {
        this.mainArtifact = dep;
        return this;
    }

    /**
     * Overrides the main component's PURL. Use this for non-Maven main components
     * such as native images or runner jars that need a
     * {@linkplain Purl#generic generic} PURL instead of a Maven one.
     *
     * @param purl the PURL identifying the main component
     * @return this config
     */
    public CoreSbomContributionConfig setMainPurl(Purl purl) {
        this.mainPurl = purl;
        return this;
    }

    /**
     * Sets the direct dependencies of the main component explicitly. These are
     * used to mark components as top-level in the generated SBOM. If not set,
     * the dependencies are obtained from the main artifact's
     * {@link ResolvedDependency#getDependencies()}.
     *
     * @param dependencies the main component's direct dependency coordinates
     * @return this config
     */
    public CoreSbomContributionConfig setMainDependencies(Collection<ArtifactCoords> dependencies) {
        this.mainDependencies = dependencies;
        return this;
    }

    /**
     * Sets the distribution directory. When set, files in this directory that are
     * not already registered as components are added with
     * {@linkplain Purl#generic generic} PURLs, and component distribution paths
     * are resolved relative to this directory.
     *
     * @param distributionDirectory the build output directory
     * @return this config
     */
    public CoreSbomContributionConfig setDistributionDirectory(Path distributionDirectory) {
        this.distDir = distributionDirectory;
        return this;
    }

    /**
     * Sets the file system path of the main runnable artifact (e.g. runner jar,
     * native image). This path is recorded on the {@link SbomContribution} and
     * also overrides the main component's file path for hash computation and
     * distribution path resolution.
     *
     * @param runnerPath path to the main runnable artifact
     * @return this config
     */
    public CoreSbomContributionConfig setMainPath(Path runnerPath) {
        this.runnerPath = runnerPath;
        return this;
    }

    /**
     * Adds an extra component descriptor not derived from the application model.
     * Use this for non-Maven components such as build-generated files that need
     * a {@linkplain Purl#generic generic} PURL.
     *
     * @param component the component descriptor
     * @return this config
     */
    public CoreSbomContributionConfig addComponent(ComponentDescriptor component) {
        additionalComponents.add(new ComponentHolder(component));
        return this;
    }

    /**
     * Adds a Maven dependency as a component. The Maven PURL, path, and scope
     * are derived from the dependency.
     *
     * @param dep the resolved Maven dependency
     * @return this config
     */
    public CoreSbomContributionConfig addComponent(ResolvedDependency dep) {
        additionalComponents.add(new ComponentHolder(dep, null, null));
        return this;
    }

    /**
     * Adds a Maven dependency as a component with a file path that differs from
     * the dependency's resolved path (e.g. after copying to a distribution directory).
     *
     * @param dep the resolved Maven dependency
     * @param path the actual file path of the component artifact
     * @return this config
     */
    public CoreSbomContributionConfig addComponent(ResolvedDependency dep, Path path) {
        additionalComponents.add(new ComponentHolder(dep, path, null));
        return this;
    }

    /**
     * Adds a Maven dependency as a component with a custom file path and optional
     * pedigree notes describing modifications (e.g. tree-shaken classes).
     *
     * @param dep the resolved Maven dependency
     * @param path the actual file path, or {@code null} to use the dependency's resolved path
     * @param pedigree pedigree notes, or {@code null}
     * @return this config
     */
    public CoreSbomContributionConfig addComponent(ResolvedDependency dep, Path path, String pedigree) {
        additionalComponents.add(new ComponentHolder(dep, path, pedigree));
        return this;
    }

    /**
     * Converts this config into an {@link SbomContribution}, creating component
     * descriptors from the application model, deduplicating by file path and
     * Maven artifact key, resolving distribution paths, and producing the
     * dependency graph.
     * <p>
     * This is the only path that designates a main application component in the SBOM.
     *
     * @return an SbomContribution representing the core application manifest
     */
    public SbomContribution toSbomContribution() {
        Map<ArtifactKey, ComponentHolder> compArtifacts = new HashMap<>();
        Map<Path, ComponentHolder> compPaths = new HashMap<>();
        List<ComponentHolder> compList = new ArrayList<>();

        addApplicationModelComponents(compArtifacts, compPaths, compList);
        ComponentHolder main = resolveMainComponent(compArtifacts, compPaths, compList);
        addAdditionalComponents(compArtifacts, compPaths, compList);
        addRemainingDistributionContent(main, compArtifacts, compPaths, compList);
        return buildContribution(main, compList);
    }

    private void addApplicationModelComponents(
            Map<ArtifactKey, ComponentHolder> compArtifacts,
            Map<Path, ComponentHolder> compPaths,
            List<ComponentHolder> compList) {
        if (applicationModel == null) {
            return;
        }
        ResolvedDependency appArtifact = applicationModel.getAppArtifact();
        addComponentHolder(toMavenDescriptor(appArtifact), appArtifact, compArtifacts, compPaths, compList);
        for (ResolvedDependency dep : applicationModel.getDependencies()) {
            addComponentHolder(toMavenDescriptor(dep), dep, compArtifacts, compPaths, compList);
        }
    }

    private ComponentHolder resolveMainComponent(
            Map<ArtifactKey, ComponentHolder> compArtifacts,
            Map<Path, ComponentHolder> compPaths,
            List<ComponentHolder> compList) {
        ComponentHolder main;
        if (mainPurl != null) {
            main = addComponentHolder(ComponentDescriptor.builder()
                    .setPurl(mainPurl)
                    .setPath(runnerPath), null, compArtifacts, compPaths, compList);
        } else if (mainArtifact != null) {
            main = addComponentHolder(toMavenDescriptor(mainArtifact), mainArtifact,
                    compArtifacts, compPaths, compList);
        } else if (!compList.isEmpty()) {
            main = compList.get(0);
        } else {
            throw new RuntimeException("mainComponent is null");
        }
        if (runnerPath != null && mainPurl == null) {
            overridePath(main, runnerPath, compPaths);
        }
        if (mainDependencies != null) {
            main.explicitDependencies = mainDependencies;
        }
        return main;
    }

    private static void overridePath(ComponentHolder holder, Path newPath,
            Map<Path, ComponentHolder> compPaths) {
        if (newPath.equals(holder.path)) {
            return;
        }
        if (holder.path != null) {
            compPaths.remove(holder.path);
        }
        holder.path = newPath;
        compPaths.put(newPath, holder);
    }

    private void addAdditionalComponents(
            Map<ArtifactKey, ComponentHolder> compArtifacts,
            Map<Path, ComponentHolder> compPaths,
            List<ComponentHolder> compList) {
        for (ComponentHolder extra : additionalComponents) {
            ComponentDescriptor desc = extra.component;
            if (desc == null && extra.dep != null) {
                ComponentDescriptor.Builder builder = toMavenDescriptor(extra.dep);
                if (extra.path != null) {
                    builder.setPath(extra.path);
                }
                if (extra.pedigree != null) {
                    builder.setPedigree(extra.pedigree);
                }
                desc = builder;
            }
            addComponentHolder(desc, extra.dep, compArtifacts, compPaths, compList);
        }
    }

    private void addRemainingDistributionContent(
            ComponentHolder main,
            Map<ArtifactKey, ComponentHolder> compArtifacts,
            Map<Path, ComponentHolder> compPaths,
            List<ComponentHolder> compList) {
        if (distDir == null) {
            return;
        }
        String version = main.component.getVersion();
        try {
            Files.walkFileTree(distDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    addComponentHolder(ComponentDescriptor.builder()
                            .setPurl(Purl.generic(file.getFileName().toString(), version))
                            .setPath(file), null, compArtifacts, compPaths, compList);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SbomContribution buildContribution(ComponentHolder main, List<ComponentHolder> compList) {
        Set<String> directDepPurls = collectDirectDependencyPurls(main);
        SbomContribution.Builder sb = SbomContribution.builder();
        sb.setRunnerPath(runnerPath);

        for (ComponentHolder holder : compList) {
            if (holder == main) {
                continue;
            }
            reconcilePath(holder);
            if (distDir != null) {
                setDistributionPath(holder, false);
            }
            defaultScope(holder);
            if (directDepPurls.contains(holder.component.getBomRef())) {
                holder.component.topLevel = true;
            }
            sb.addComponent(holder.component.ensureImmutable());
            addDependency(holder, sb);
        }

        if (distDir != null) {
            setDistributionPath(main, true);
        }
        defaultScope(main);
        ComponentDescriptor mainComponent = main.component.ensureImmutable();
        sb.setMainComponentBomRef(mainComponent.getBomRef());
        sb.addComponent(mainComponent);
        addDependency(main, sb);

        return sb.build();
    }

    private static ComponentHolder addComponentHolder(
            ComponentDescriptor component, ResolvedDependency dep,
            Map<ArtifactKey, ComponentHolder> compArtifacts,
            Map<Path, ComponentHolder> compPaths,
            List<ComponentHolder> compList) {
        if (component.getScope() == null && dep != null) {
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
            holder = new ComponentHolder(component);
            holder.dep = dep;
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

    private static ComponentDescriptor.Builder toMavenDescriptor(ResolvedDependency d) {
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

    private static Purl mavenPurl(ArtifactCoords coords) {
        return Purl.maven(coords.getGroupId(), coords.getArtifactId(), coords.getVersion(),
                coords.getType(),
                coords.getClassifier().isEmpty() ? null : coords.getClassifier());
    }

    private static Set<String> collectDirectDependencyPurls(ComponentHolder main) {
        Collection<ArtifactCoords> deps = main.explicitDependencies;
        if (deps == null || deps.isEmpty()) {
            deps = main.dep != null ? main.dep.getDependencies() : null;
        }
        if (deps == null || deps.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>(deps.size());
        for (ArtifactCoords coord : deps) {
            result.add(mavenPurl(coord).toString());
        }
        return result;
    }

    private static void reconcilePath(ComponentHolder holder) {
        if (holder.path != null && !holder.path.equals(holder.component.getPath())) {
            ComponentDescriptor.Builder builder = new ComponentDescriptor.Builder(holder.component);
            builder.setPath(holder.path);
            holder.component = builder;
        }
    }

    private static void addDependency(ComponentHolder holder, SbomContribution.Builder sb) {
        Collection<ArtifactCoords> deps = holder.explicitDependencies;
        if (deps == null || deps.isEmpty()) {
            deps = holder.dep != null ? holder.dep.getDependencies() : null;
        }
        if (deps == null || deps.isEmpty()) {
            return;
        }
        List<String> depBomRefs = new ArrayList<>(deps.size());
        for (ArtifactCoords depCoords : deps) {
            depBomRefs.add(mavenPurl(depCoords).toString());
        }
        sb.addDependency(ComponentDependencies.of(holder.component.getBomRef(), depBomRefs));
    }

    private static void defaultScope(ComponentHolder holder) {
        if (holder.component.scope == null) {
            holder.component.scope = ComponentDescriptor.SCOPE_RUNTIME;
        }
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
}
