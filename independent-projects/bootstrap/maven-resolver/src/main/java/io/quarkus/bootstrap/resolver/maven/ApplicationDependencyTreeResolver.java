package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.newDependencyBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;

/**
 * The legacy {@link io.quarkus.bootstrap.model.ApplicationModel} resolver implementation.
 * <p>
 * This implementation is kept just in case an issue is found in the new default implementation, which is
 * {@link ApplicationDependencyResolver},
 * and can be enabled by setting {@code quarkus.bootstrap.legacy-model-resolver} system or POM property to {@code true}.
 *
 * @deprecated since 3.19.0
 */
@Deprecated(since = "3.19.0", forRemoval = true)
public class ApplicationDependencyTreeResolver {

    private static final Logger log = Logger.getLogger(ApplicationDependencyTreeResolver.class);

    private static final String QUARKUS_RUNTIME_ARTIFACT = "quarkus.runtime";
    private static final String QUARKUS_EXTENSION_DEPENDENCY = "quarkus.ext";

    /* @formatter:off */
    private static final byte COLLECT_TOP_EXTENSION_RUNTIME_NODES = 0b0001;
    private static final byte COLLECT_DIRECT_DEPS =                 0b0010;
    private static final byte COLLECT_RELOADABLE_MODULES =          0b0100;
    private static final byte COLLECT_DEPLOYMENT_INJECTION_POINTS = 0b1000;
    /* @formatter:on */

    // this is a temporary option, to enable the previous way of initializing runtime classpath dependencies
    private static final boolean CONVERGED_TREE_ONLY = PropertyUtils.getBoolean("quarkus.bootstrap.converged-tree-only", false);

    public static ApplicationDependencyTreeResolver newInstance() {
        return new ApplicationDependencyTreeResolver();
    }

    public static Artifact getRuntimeArtifact(DependencyNode dep) {
        return (Artifact) dep.getData().get(QUARKUS_RUNTIME_ARTIFACT);
    }

    private byte walkingFlags = COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS | COLLECT_DEPLOYMENT_INJECTION_POINTS;
    private final List<ExtensionDependency> deploymentInjectionPoints = new ArrayList<>();
    private ExtensionDependency currentTopLevelExtension;
    private final Map<ArtifactKey, ExtensionInfo> allExtensions = new HashMap<>();
    private List<ConditionalDependency> conditionalDepsToProcess = new ArrayList<>();
    private final Deque<Collection<Exclusion>> exclusionStack = new ArrayDeque<>();

    private final Map<ArtifactCoords, Set<ArtifactKey>> artifactDeps = new HashMap<>();

    private MavenArtifactResolver resolver;
    private List<Dependency> managedDeps;
    private ApplicationModelBuilder appBuilder;
    private boolean collectReloadableModules;
    private Consumer<String> buildTreeConsumer;
    private List<Dependency> collectCompileOnly;
    private boolean runtimeModelOnly;
    private boolean devMode;

    public ApplicationDependencyTreeResolver setArtifactResolver(MavenArtifactResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    public ApplicationDependencyTreeResolver setApplicationModelBuilder(ApplicationModelBuilder appBuilder) {
        this.appBuilder = appBuilder;
        return this;
    }

    public ApplicationDependencyTreeResolver setCollectReloadableModules(boolean collectReloadableModules) {
        this.collectReloadableModules = collectReloadableModules;
        return this;
    }

    public ApplicationDependencyTreeResolver setBuildTreeConsumer(Consumer<String> buildTreeConsumer) {
        this.buildTreeConsumer = buildTreeConsumer;
        return this;
    }

    /**
     * In addition to resolving dependencies for the build classpath, also resolve these compile-only dependencies
     * and add them to the application model as {@link DependencyFlags#COMPILE_ONLY}.
     *
     * @param collectCompileOnly compile-only dependencies to add to the model
     * @return self
     */
    public ApplicationDependencyTreeResolver setCollectCompileOnly(List<Dependency> collectCompileOnly) {
        this.collectCompileOnly = collectCompileOnly;
        return this;
    }

    /**
     * Whether to limit the resulting {@link io.quarkus.bootstrap.model.ApplicationModel} to the runtime dependencies.
     *
     * @param runtimeModelOnly whether to limit the resulting application model to the runtime dependencies
     * @return self
     */
    public ApplicationDependencyTreeResolver setRuntimeModelOnly(boolean runtimeModelOnly) {
        this.runtimeModelOnly = runtimeModelOnly;
        return this;
    }

    /**
     * Whether an application model is resolved for dev mode
     *
     * @param devMode whether an application model is resolved for dev mode
     * @return self
     */
    public ApplicationDependencyTreeResolver setDevMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    public void resolve(CollectRequest collectRtDepsRequest) throws AppModelResolverException {

        this.managedDeps = collectRtDepsRequest.getManagedDependencies();
        DependencyNode root = resolveRuntimeDeps(collectRtDepsRequest);

        if (collectReloadableModules) {
            setWalkingFlag(COLLECT_RELOADABLE_MODULES);
        }
        // we need to be able to take into account whether the deployment dependencies are on an optional dependency branch
        // for that we are going to use a custom dependency selector and re-initialize the resolver to use it
        final MavenArtifactResolver originalResolver = resolver;
        final RepositorySystemSession originalSession = resolver.getSession();
        final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(originalSession);
        session.setDependencySelector(
                DeploymentDependencySelector.ensureDeploymentDependencySelector(session.getDependencySelector()));
        try {
            this.resolver = new MavenArtifactResolver(new BootstrapMavenContext(BootstrapMavenContext.config()
                    .setRepositorySystem(resolver.getSystem())
                    .setRepositorySystemSession(session)
                    .setRemoteRepositories(resolver.getRepositories())
                    .setRemoteRepositoryManager(resolver.getRemoteRepositoryManager())
                    .setCurrentProject(resolver.getMavenContext().getCurrentProject())
                    .setWorkspaceDiscovery(false)));
        } catch (BootstrapMavenException e) {
            throw new BootstrapDependencyProcessingException("Failed to initialize deployment dependencies resolver",
                    e);
        }

        this.managedDeps = managedDeps.isEmpty() ? new ArrayList<>() : managedDeps;

        visitRuntimeDependencies(root.getChildren());
        enableConditionalDeps();

        if (!runtimeModelOnly) {
            for (ExtensionDependency extDep : deploymentInjectionPoints) {
                injectDeploymentDependencies(extDep);
            }
        }

        root = normalize(originalSession, root);
        // add deployment dependencies
        new BuildDependencyGraphVisitor(originalResolver, appBuilder, buildTreeConsumer).visit(root);

        if (!CONVERGED_TREE_ONLY && collectReloadableModules) {
            for (ResolvedDependencyBuilder db : appBuilder.getDependencies()) {
                if (db.isFlagSet(DependencyFlags.RELOADABLE | DependencyFlags.VISITED)) {
                    continue;
                }
                clearReloadableFlag(db);
            }
        }

        for (ResolvedDependencyBuilder db : appBuilder.getDependencies()) {
            db.clearFlag(DependencyFlags.VISITED);
            appBuilder.addDependency(db);
        }

        collectPlatformProperties();
        if (!runtimeModelOnly) {
            collectCompileOnly(collectRtDepsRequest, root);
        }
    }

    private void enableConditionalDeps() {
        if (!conditionalDepsToProcess.isEmpty()) {
            List<ConditionalDependency> unsatisfiedConditionalDeps = new ArrayList<>();
            while (!conditionalDepsToProcess.isEmpty()) {
                final List<ConditionalDependency> tmp = unsatisfiedConditionalDeps;
                unsatisfiedConditionalDeps = conditionalDepsToProcess;
                conditionalDepsToProcess = tmp;
                final int totalConditionsToProcess = unsatisfiedConditionalDeps.size();
                final Iterator<ConditionalDependency> i = unsatisfiedConditionalDeps.iterator();
                while (i.hasNext()) {
                    final ConditionalDependency cd = i.next();
                    if (cd.isSatisfied()) {
                        i.remove();
                        cd.activate();
                    }
                }
                if (totalConditionsToProcess == unsatisfiedConditionalDeps.size()) {
                    // none of the dependencies was satisfied
                    break;
                }
                conditionalDepsToProcess.addAll(unsatisfiedConditionalDeps);
                unsatisfiedConditionalDeps.clear();
            }
        }
    }

    /**
     * Resolves and adds compile-only dependencies to the application model with the {@link DependencyFlags#COMPILE_ONLY} flag.
     * Compile-only dependencies are resolved as direct dependencies of the root with all the previously resolved dependencies
     * enforced as version constraints to make sure compile-only dependencies do not override runtime dependencies of the final
     * application.
     *
     * @param collectRtDepsRequest original runtime dependencies collection request
     * @param root the root node of the Quarkus build time dependency tree
     * @throws BootstrapMavenException in case of a failure
     */
    private void collectCompileOnly(CollectRequest collectRtDepsRequest, DependencyNode root) throws BootstrapMavenException {
        if (collectCompileOnly.isEmpty()) {
            return;
        }
        // add all the build time dependencies as version constraints
        var depStack = new ArrayDeque<List<DependencyNode>>();
        var children = root.getChildren();
        while (children != null) {
            for (DependencyNode node : children) {
                managedDeps.add(node.getDependency());
                if (!node.getChildren().isEmpty()) {
                    depStack.add(node.getChildren());
                }
            }
            children = depStack.poll();
        }
        final CollectRequest request = new CollectRequest()
                .setDependencies(collectCompileOnly)
                .setManagedDependencies(managedDeps)
                .setRepositories(collectRtDepsRequest.getRepositories());
        if (collectRtDepsRequest.getRoot() != null) {
            request.setRoot(collectRtDepsRequest.getRoot());
        } else {
            request.setRootArtifact(collectRtDepsRequest.getRootArtifact());
        }

        try {
            root = resolver.getSystem().collectDependencies(resolver.getSession(), request).getRoot();
        } catch (DependencyCollectionException e) {
            throw new BootstrapDependencyProcessingException(
                    "Failed to collect compile-only dependencies of " + root.getArtifact(), e);
        }
        children = root.getChildren();
        int flags = DependencyFlags.DIRECT | DependencyFlags.COMPILE_ONLY;
        while (children != null) {
            for (DependencyNode node : children) {
                var extInfo = getExtensionInfoOrNull(node.getArtifact(), node.getRepositories());
                var dep = appBuilder.getDependency(getKey(node.getArtifact()));
                if (dep == null) {
                    dep = newDependencyBuilder(node, resolver).setFlags(flags);
                    if (extInfo != null) {
                        dep.setFlags(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
                        if (dep.isFlagSet(DependencyFlags.DIRECT)) {
                            dep.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                        }
                    }
                    appBuilder.addDependency(dep);
                } else {
                    dep.setFlags(DependencyFlags.COMPILE_ONLY);
                }
                if (!node.getChildren().isEmpty()) {
                    depStack.add(node.getChildren());
                }
            }
            flags = DependencyFlags.COMPILE_ONLY;
            children = depStack.poll();
        }
    }

    private void collectPlatformProperties() throws AppModelResolverException {
        final PlatformImportsImpl platformReleases = new PlatformImportsImpl();
        for (Dependency d : managedDeps) {
            final Artifact artifact = d.getArtifact();
            final String extension = artifact.getExtension();
            final String artifactId = artifact.getArtifactId();
            if ("json".equals(extension)
                    && artifactId.endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)) {
                platformReleases.addPlatformDescriptor(artifact.getGroupId(), artifactId, artifact.getClassifier(), extension,
                        artifact.getVersion());
            } else if ("properties".equals(artifact.getExtension())
                    && artifactId.endsWith(BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX)) {
                platformReleases.addPlatformProperties(artifact.getGroupId(), artifactId, artifact.getClassifier(), extension,
                        artifact.getVersion(), resolver.resolve(artifact).getArtifact().getFile().toPath());
            }
        }
        appBuilder.setPlatformImports(platformReleases);
    }

    private void clearReloadableFlag(ResolvedDependencyBuilder db) {
        final Set<ArtifactKey> deps = artifactDeps.get(db.getArtifactCoords());
        if (deps == null || deps.isEmpty()) {
            return;
        }
        for (ArtifactKey key : deps) {
            final ResolvedDependencyBuilder dep = appBuilder.getDependency(key);
            if (dep == null || dep.isFlagSet(DependencyFlags.VISITED)) {
                continue;
            }
            dep.setFlags(DependencyFlags.VISITED);
            dep.clearFlag(DependencyFlags.RELOADABLE);
            clearReloadableFlag(dep);
        }
    }

    private DependencyNode normalize(RepositorySystemSession session, DependencyNode root) throws AppModelResolverException {
        final DependencyGraphTransformationContext context = new SimpleDependencyGraphTransformationContext(session);
        try {
            // add conflict IDs to the added deployments
            root = new ConflictMarker().transformGraph(root, context);
            // resolves version conflicts
            root = new ConflictIdSorter().transformGraph(root, context);
            root = session.getDependencyGraphTransformer().transformGraph(root, context);
        } catch (RepositoryException e) {
            throw new AppModelResolverException("Failed to normalize the dependency graph", e);
        }
        return root;
    }

    private DependencyNode resolveRuntimeDeps(CollectRequest request) throws AppModelResolverException {
        var session = resolver.getSession();
        if (!CONVERGED_TREE_ONLY && collectReloadableModules) {
            final DefaultRepositorySystemSession mutableSession;
            mutableSession = new DefaultRepositorySystemSession(resolver.getSession());
            mutableSession.setDependencyGraphTransformer(new DependencyGraphTransformer() {

                @Override
                public DependencyNode transformGraph(DependencyNode node, DependencyGraphTransformationContext context)
                        throws RepositoryException {
                    final Map<DependencyNode, DependencyNode> visited = new IdentityHashMap<>();
                    for (DependencyNode c : node.getChildren()) {
                        walk(c, visited);
                    }
                    return resolver.getSession().getDependencyGraphTransformer().transformGraph(node, context);
                }

                private void walk(DependencyNode node, Map<DependencyNode, DependencyNode> visited) {
                    if (visited.put(node, node) != null || node.getChildren().isEmpty()) {
                        return;
                    }
                    final Set<ArtifactKey> deps = artifactDeps
                            .computeIfAbsent(DependencyUtils.getCoords(node.getArtifact()),
                                    k -> new HashSet<>(node.getChildren().size()));
                    for (DependencyNode c : node.getChildren()) {
                        deps.add(getKey(c.getArtifact()));
                        walk(c, visited);
                    }
                }
            });
            session = mutableSession;
        }
        try {
            return resolver.getSystem().resolveDependencies(session,
                    new DependencyRequest().setCollectRequest(request))
                    .getRoot();
        } catch (DependencyResolutionException e) {
            final Artifact a = request.getRoot() == null ? request.getRootArtifact() : request.getRoot().getArtifact();
            throw new BootstrapMavenException("Failed to resolve dependencies for " + a, e);
        }
    }

    private boolean isRuntimeArtifact(ArtifactKey key) {
        final ResolvedDependencyBuilder dep = appBuilder.getDependency(key);
        return dep != null && dep.isFlagSet(DependencyFlags.RUNTIME_CP);
    }

    private void visitRuntimeDependencies(List<DependencyNode> list) {
        for (DependencyNode n : list) {
            visitRuntimeDependency(n);
        }
    }

    private void visitRuntimeDependency(DependencyNode node) {

        final byte prevWalkingFlags = walkingFlags;

        final boolean popExclusions = !node.getDependency().getExclusions().isEmpty();
        if (popExclusions) {
            exclusionStack.addLast(node.getDependency().getExclusions());
        }

        Artifact artifact = node.getArtifact();
        final ArtifactKey key = getKey(artifact);
        ResolvedDependencyBuilder dep = appBuilder.getDependency(key);
        if (dep == null) {
            artifact = resolve(artifact, node.getRepositories());
        }

        try {
            final ExtensionDependency extDep = getExtensionDependencyOrNull(node, artifact);

            if (dep == null) {
                // in case it was relocated it might not survive conflict resolution in the deployment graph
                if (!node.getRelocations().isEmpty()) {
                    ((DefaultDependencyNode) node).setRelocations(List.of());
                }
                WorkspaceModule module = null;
                if (resolver.getProjectModuleResolver() != null) {
                    module = resolver.getProjectModuleResolver().getProjectModule(artifact.getGroupId(),
                            artifact.getArtifactId(), artifact.getVersion());
                }
                dep = DependencyUtils.toAppArtifact(artifact, module)
                        .setOptional(node.getDependency().isOptional())
                        .setScope(node.getDependency().getScope())
                        .setDirect(isWalkingFlagOn(COLLECT_DIRECT_DEPS))
                        .setRuntimeCp()
                        .setDeploymentCp();
                if (JavaScopes.PROVIDED.equals(dep.getScope())) {
                    dep.setFlags(DependencyFlags.COMPILE_ONLY);
                }
                if (extDep != null) {
                    dep.setRuntimeExtensionArtifact();
                    if (isWalkingFlagOn(COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
                        dep.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                    }
                    managedDeps.add(new Dependency(extDep.info.deploymentArtifact, JavaScopes.COMPILE));
                    collectConditionalDependencies(extDep);
                }
                if (isWalkingFlagOn(COLLECT_RELOADABLE_MODULES)) {
                    if (module != null) {
                        dep.setReloadable();
                        appBuilder.addReloadableWorkspaceModule(key);
                    } else {
                        clearWalkingFlag(COLLECT_RELOADABLE_MODULES);
                    }
                }
                appBuilder.addDependency(dep);
            }
            clearWalkingFlag(COLLECT_DIRECT_DEPS);

            if (extDep != null) {
                extDep.info.ensureActivated(appBuilder);
                visitExtensionDependency(extDep);
            }
            visitRuntimeDependencies(node.getChildren());
        } catch (DeploymentInjectionException e) {
            throw e;
        } catch (Exception t) {
            throw new DeploymentInjectionException("Failed to inject extension deployment dependencies", t);
        }

        if (popExclusions) {
            exclusionStack.pollLast();
        }
        walkingFlags = prevWalkingFlags;
    }

    private ExtensionDependency getExtensionDependencyOrNull(DependencyNode node, Artifact artifact)
            throws BootstrapDependencyProcessingException {
        ExtensionDependency extDep = ExtensionDependency.get(node);
        if (extDep != null) {
            return extDep;
        }
        final ExtensionInfo extInfo = getExtensionInfoOrNull(artifact, node.getRepositories());
        if (extInfo != null) {
            final Collection<Exclusion> exclusions;
            if (exclusionStack.isEmpty()) {
                exclusions = List.of();
            } else if (exclusionStack.size() == 1) {
                exclusions = exclusionStack.peekLast();
            } else {
                exclusions = new ArrayList<>();
                for (Collection<Exclusion> set : exclusionStack) {
                    exclusions.addAll(set);
                }
            }
            return new ExtensionDependency(extInfo, node, exclusions);
        }
        return null;
    }

    private void visitExtensionDependency(ExtensionDependency extDep) {
        if (clearWalkingFlag(COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
            currentTopLevelExtension = extDep;
        } else if (currentTopLevelExtension != null) {
            currentTopLevelExtension.addExtensionDependency(extDep);
        } // else it'd be an unexpected situation

        if (clearWalkingFlag(COLLECT_DEPLOYMENT_INJECTION_POINTS)) {
            deploymentInjectionPoints.add(extDep);
        }
    }

    private void collectConditionalDependencies(ExtensionDependency dependent)
            throws BootstrapDependencyProcessingException {
        if (dependent.info.conditionalDeps.length == 0 || dependent.conditionalDepsQueued) {
            return;
        }
        dependent.conditionalDepsQueued = true;

        final DependencySelector selector = dependent.exclusions == null ? null
                : new ExclusionDependencySelector(dependent.exclusions);
        for (Artifact conditionalArtifact : dependent.info.conditionalDeps) {
            if (selector != null && !selector.selectDependency(new Dependency(conditionalArtifact, JavaScopes.RUNTIME))) {
                continue;
            }
            conditionalArtifact = resolve(conditionalArtifact, dependent.runtimeNode.getRepositories());
            final ExtensionInfo condExtInfo = getExtensionInfoOrNull(conditionalArtifact,
                    dependent.runtimeNode.getRepositories());
            if (condExtInfo != null && condExtInfo.activated) {
                continue;
            }
            final ConditionalDependency conditionalDep = new ConditionalDependency(conditionalArtifact, condExtInfo,
                    dependent);
            conditionalDepsToProcess.add(conditionalDep);
            if (condExtInfo != null) {
                collectConditionalDependencies(conditionalDep.getExtensionDependency());
            }
        }
    }

    private ExtensionInfo getExtensionInfoOrNull(Artifact artifact, List<RemoteRepository> repos)
            throws BootstrapDependencyProcessingException {
        if (!artifact.getExtension().equals(ArtifactCoords.TYPE_JAR)) {
            return null;
        }
        final ArtifactKey extKey = getKey(artifact);
        ExtensionInfo ext = allExtensions.get(extKey);
        if (ext != null) {
            return ext;
        }

        artifact = resolve(artifact, repos);
        final Path path = artifact.getFile().toPath();
        final Properties descriptor = PathTree.ofDirectoryOrArchive(path).apply(BootstrapConstants.DESCRIPTOR_PATH,
                ApplicationDependencyTreeResolver::readExtensionProperties);
        if (descriptor != null) {
            ext = new ExtensionInfo(artifact, descriptor, devMode);
            allExtensions.put(extKey, ext);
        }
        return ext;
    }

    private static Properties readExtensionProperties(PathVisit visit) {
        if (visit == null) {
            return null;
        }
        try {
            final Properties rtProps = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(visit.getPath())) {
                rtProps.load(reader);
            }
            return rtProps;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void injectDeploymentDependencies(ExtensionDependency extDep)
            throws BootstrapDependencyProcessingException {
        log.debugf("Injecting deployment dependency %s", extDep.info.deploymentArtifact);
        final DependencyNode deploymentNode = collectDependencies(extDep.info.deploymentArtifact, extDep.exclusions,
                extDep.runtimeNode.getRepositories());
        if (deploymentNode.getChildren().isEmpty()) {
            throw new BootstrapDependencyProcessingException(
                    "Failed to collect dependencies of " + deploymentNode.getArtifact()
                            + ": either its POM could not be resolved from the available Maven repositories "
                            + "or the artifact does not have any dependencies while at least a dependency on the runtime artifact "
                            + extDep.info.runtimeArtifact + " is expected");
        }

        if (resolver.getProjectModuleResolver() != null && collectReloadableModules) {
            clearReloadable(deploymentNode);
        }

        extDep.replaceRuntimeExtensionNodes(deploymentNode);
        if (!extDep.presentInTargetGraph) {
            throw new BootstrapDependencyProcessingException(
                    "Quarkus extension deployment artifact " + deploymentNode.getArtifact()
                            + " does not appear to depend on the corresponding runtime artifact "
                            + extDep.info.runtimeArtifact);
        }

        final DependencyNode runtimeNode = extDep.runtimeNode;
        runtimeNode.setData(QUARKUS_RUNTIME_ARTIFACT, runtimeNode.getArtifact());
        runtimeNode.setArtifact(deploymentNode.getArtifact());
        runtimeNode.getDependency().setArtifact(deploymentNode.getArtifact());
        runtimeNode.setChildren(deploymentNode.getChildren());
    }

    private void clearReloadable(DependencyNode node) {
        for (DependencyNode child : node.getChildren()) {
            clearReloadable(child);
        }
        final ResolvedDependencyBuilder dep = appBuilder.getDependency(getKey(node.getArtifact()));
        if (dep != null) {
            dep.clearFlag(DependencyFlags.RELOADABLE);
        }
    }

    private DependencyNode collectDependencies(Artifact artifact, Collection<Exclusion> exclusions,
            List<RemoteRepository> repos) {
        final CollectRequest request;
        if (managedDeps.isEmpty()) {
            request = new CollectRequest()
                    .setRoot(new Dependency(artifact, JavaScopes.COMPILE, false, exclusions))
                    .setRepositories(repos);
        } else {
            final ArtifactDescriptorResult descr;
            try {
                descr = resolver.resolveDescriptor(artifact, repos);
            } catch (BootstrapMavenException e) {
                throw new DeploymentInjectionException("Failed to resolve descriptor for " + artifact, e);
            }
            final List<Dependency> mergedManagedDeps = new ArrayList<>(
                    managedDeps.size() + descr.getManagedDependencies().size());
            final Map<ArtifactKey, String> managedVersions = new HashMap<>(managedDeps.size());
            for (Dependency dep : managedDeps) {
                managedVersions.put(DependencyUtils.getKey(dep.getArtifact()), dep.getArtifact().getVersion());
                mergedManagedDeps.add(dep);
            }
            for (Dependency dep : descr.getManagedDependencies()) {
                final ArtifactKey key = DependencyUtils.getKey(dep.getArtifact());
                if (!managedVersions.containsKey(key)) {
                    mergedManagedDeps.add(dep);
                }
            }

            var directDeps = DependencyUtils.mergeDeps(List.of(), descr.getDependencies(), managedVersions,
                    Set.of(JavaScopes.PROVIDED, JavaScopes.TEST));

            request = new CollectRequest()
                    .setDependencies(directDeps)
                    .setManagedDependencies(mergedManagedDeps)
                    .setRepositories(repos);
            if (exclusions.isEmpty()) {
                request.setRootArtifact(artifact);
            } else {
                request.setRoot(new Dependency(artifact, JavaScopes.COMPILE, false, exclusions));
            }
        }
        try {
            return resolver.getSystem().collectDependencies(resolver.getSession(), request).getRoot();
        } catch (DependencyCollectionException e) {
            throw new DeploymentInjectionException("Failed to collect dependencies for " + artifact, e);
        }
    }

    private Artifact resolve(Artifact artifact, List<RemoteRepository> repos) {
        if (artifact.getFile() != null) {
            return artifact;
        }
        try {
            return resolver.getSystem().resolveArtifact(resolver.getSession(),
                    new ArtifactRequest()
                            .setArtifact(artifact)
                            .setRepositories(repos))
                    .getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new DeploymentInjectionException("Failed to resolve artifact " + artifact, e);
        }
    }

    private void setWalkingFlag(byte flag) {
        walkingFlags |= flag;
    }

    private boolean isWalkingFlagOn(byte flag) {
        return (walkingFlags & flag) == flag;
    }

    /**
     * Clears the flags and returns {@code true}, if the flags were actually on and cleared.
     *
     * @param flags the flags to clear
     * @return true if the flags were on and cleared, false if the flags were not on
     */
    private boolean clearWalkingFlag(byte flags) {
        return walkingFlags != (walkingFlags &= (byte) (walkingFlags ^ flags));
    }

    private static class ExtensionDependency {

        static ExtensionDependency get(DependencyNode node) {
            return (ExtensionDependency) node.getData().get(QUARKUS_EXTENSION_DEPENDENCY);
        }

        final ExtensionInfo info;
        final DependencyNode runtimeNode;
        final Collection<Exclusion> exclusions;
        boolean conditionalDepsQueued;
        private List<ExtensionDependency> extDeps;
        private boolean presentInTargetGraph;

        private ExtensionDependency(ExtensionInfo info, DependencyNode node, Collection<Exclusion> exclusions) {
            this.runtimeNode = node;
            this.info = info;
            this.exclusions = exclusions;

            @SuppressWarnings("unchecked")
            final Map<Object, Object> data = (Map<Object, Object>) node.getData();
            if (data.isEmpty()) {
                node.setData(QUARKUS_EXTENSION_DEPENDENCY, this);
            } else if (data.put(QUARKUS_EXTENSION_DEPENDENCY, this) != null) {
                throw new IllegalStateException(
                        "Dependency node " + node + " has already been associated with an extension dependency");
            }
        }

        private void addExtensionDependency(ExtensionDependency dep) {
            if (extDeps == null) {
                extDeps = new ArrayList<>();
            }
            extDeps.add(dep);
        }

        private void replaceRuntimeExtensionNodes(DependencyNode deploymentNode) {
            var deploymentVisitor = new OrderedDependencyVisitor(deploymentNode);
            // skip the root node
            deploymentVisitor.next();
            int nodesToReplace = extDeps == null ? 1 : extDeps.size() + 1;
            while (deploymentVisitor.hasNext() && nodesToReplace > 0) {
                deploymentVisitor.next();
                if (replaceRuntimeNode(deploymentVisitor)) {
                    --nodesToReplace;
                } else if (extDeps != null) {
                    for (int i = 0; i < extDeps.size(); ++i) {
                        if (extDeps.get(i).replaceRuntimeNode(deploymentVisitor)) {
                            --nodesToReplace;
                            break;
                        }
                    }
                }
            }
        }

        private boolean replaceRuntimeNode(OrderedDependencyVisitor depVisitor) {
            if (!presentInTargetGraph && isSameKey(runtimeNode.getArtifact(), depVisitor.getCurrent().getArtifact())) {
                // we are not comparing the version in the above condition because the runtime version
                // may appear to be different from the deployment one and that's ok
                // e.g. the version of the runtime artifact could be managed by a BOM
                // but overridden by the user in the project config. The way the deployment deps
                // are resolved here, the deployment version of the runtime artifact will be the one from the BOM.
                var inserted = new DefaultDependencyNode(runtimeNode);
                inserted.setChildren(runtimeNode.getChildren());
                depVisitor.replaceCurrent(inserted);
                presentInTargetGraph = true;
                return true;
            }
            return false;
        }
    }

    private class ConditionalDependency {

        final Artifact artifact;
        final ExtensionInfo info;
        final ExtensionDependency dependent;
        private ExtensionDependency dependency;
        private boolean activated;

        private ConditionalDependency(Artifact artifact, ExtensionInfo info, ExtensionDependency dependent) {
            this.artifact = Objects.requireNonNull(artifact, "artifact is null");
            this.info = info;
            this.dependent = dependent;
        }

        ExtensionDependency getExtensionDependency() {
            if (dependency == null && info != null) {
                dependency = new ExtensionDependency(info, initDependencyNode(), dependent.exclusions);
            }
            return dependency;
        }

        private DependencyNode initDependencyNode() {
            final DefaultDependencyNode rtNode = new DefaultDependencyNode(new Dependency(artifact, JavaScopes.COMPILE));
            rtNode.setVersion(new BootstrapArtifactVersion(artifact.getVersion()));
            rtNode.setVersionConstraint(new BootstrapArtifactVersionConstraint(
                    new BootstrapArtifactVersion(artifact.getVersion())));
            rtNode.setRepositories(dependent.runtimeNode.getRepositories());
            return rtNode;
        }

        void activate() {
            if (activated) {
                return;
            }
            activated = true;
            final DependencyNode originalNode = collectDependencies(artifact, dependent.exclusions,
                    dependent.runtimeNode.getRepositories());

            clearWalkingFlag((byte) (COLLECT_DIRECT_DEPS | COLLECT_TOP_EXTENSION_RUNTIME_NODES));
            final ExtensionDependency extDep = getExtensionDependency();
            final DependencyNode rtNode = extDep == null ? initDependencyNode() : extDep.runtimeNode;
            ((DefaultDependencyNode) rtNode).setRepositories(originalNode.getRepositories());
            // if this node has conditional dependencies on its own, they may have been activated by this time
            // in which case they would be included into its children
            if (rtNode.getChildren().isEmpty()) {
                rtNode.setChildren(originalNode.getChildren());
            } else {
                rtNode.getChildren().addAll(originalNode.getChildren());
            }
            currentTopLevelExtension = null;
            setWalkingFlag(COLLECT_DEPLOYMENT_INJECTION_POINTS);
            visitRuntimeDependency(rtNode);
            dependent.runtimeNode.getChildren().add(rtNode);
        }

        boolean isSatisfied() {
            if (info == null || info.dependencyCondition == null) {
                return true;
            }
            for (ArtifactKey key : info.dependencyCondition) {
                if (!isRuntimeArtifact(key)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean isSameKey(Artifact a1, Artifact a2) {
        return a2.getArtifactId().equals(a1.getArtifactId())
                && a2.getGroupId().equals(a1.getGroupId())
                && a2.getClassifier().equals(a1.getClassifier())
                && a2.getExtension().equals(a1.getExtension());
    }
}
