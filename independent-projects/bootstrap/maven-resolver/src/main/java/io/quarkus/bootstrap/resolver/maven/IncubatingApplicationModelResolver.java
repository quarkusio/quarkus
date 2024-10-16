package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getCoords;
import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.getWinner;
import static io.quarkus.bootstrap.util.DependencyUtils.hasWinner;
import static io.quarkus.bootstrap.util.DependencyUtils.newDependencyBuilder;
import static io.quarkus.bootstrap.util.DependencyUtils.toArtifact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathTree;

public class IncubatingApplicationModelResolver {

    private static final Logger log = Logger.getLogger(IncubatingApplicationModelResolver.class);

    private static final String QUARKUS_RUNTIME_ARTIFACT = "quarkus.runtime";
    private static final String QUARKUS_EXTENSION_DEPENDENCY = "quarkus.ext";

    private static final String INCUBATING_MODEL_RESOLVER = "quarkus.bootstrap.incubating-model-resolver";

    /* @formatter:off */
    private static final byte COLLECT_TOP_EXTENSION_RUNTIME_NODES = 0b001;
    private static final byte COLLECT_DIRECT_DEPS =                 0b010;
    private static final byte COLLECT_RELOADABLE_MODULES =          0b100;
    /* @formatter:on */

    private static final Artifact[] NO_ARTIFACTS = new Artifact[0];

    /**
     * Temporary method that will be removed once this implementation becomes the default.
     * <p>
     * Returns {@code true} if system or POM property {@code quarkus.bootstrap.incubating-model-resolver}
     * is set to {@code true}.
     *
     * @return true if this implementation is enabled
     */
    public static boolean isIncubatingEnabled(Properties projectProperties) {
        return Boolean.parseBoolean(getIncubatingModelResolverProperty(projectProperties));
    }

    /**
     * Temporary method that will be removed once this implementation becomes the default.
     * <p>
     * Calls {@link #getIncubatingModelResolverProperty(Properties)} and checks whether the returned value
     * equals the passed in {@code value}.
     *
     * @return true if value of quarkus.bootstrap.incubating-model-resolver property is equal to the passed in value
     */
    public static boolean isIncubatingModelResolverProperty(Properties projectProperties, String value) {
        Objects.requireNonNull(value);
        return value.equals(getIncubatingModelResolverProperty(projectProperties));
    }

    /**
     * Temporary method that will be removed once this implementation becomes the default.
     * <p>
     * Returns value of system or POM property {@code quarkus.bootstrap.incubating-model-resolver}.
     * The system property is checked first and if its value is not {@code null}, it's returned.
     * Otherwise, the value of POM property is returned as the result.
     *
     * @return value of system or POM property quarkus.bootstrap.incubating-model-resolver or null if it's not set
     */
    public static String getIncubatingModelResolverProperty(Properties projectProperties) {
        var value = System.getProperty(INCUBATING_MODEL_RESOLVER);
        if (value != null || projectProperties == null) {
            return value;
        }
        return String.valueOf(projectProperties.get(INCUBATING_MODEL_RESOLVER));
    }

    public static IncubatingApplicationModelResolver newInstance() {
        return new IncubatingApplicationModelResolver();
    }

    private final ExtensionInfo EXT_INFO_NONE = new ExtensionInfo();

    private final List<ExtensionDependency> topExtensionDeps = new ArrayList<>();
    private final Map<ArtifactKey, ExtensionInfo> allExtensions = new ConcurrentHashMap<>();
    private List<ConditionalDependency> conditionalDepsToProcess = new ArrayList<>();

    private MavenArtifactResolver resolver;
    private List<Dependency> managedDeps;
    private ApplicationModelBuilder appBuilder;
    private boolean collectReloadableModules;
    private DependencyLoggingConfig depLogging;
    private List<Dependency> collectCompileOnly;

    public IncubatingApplicationModelResolver setArtifactResolver(MavenArtifactResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    public IncubatingApplicationModelResolver setApplicationModelBuilder(ApplicationModelBuilder appBuilder) {
        this.appBuilder = appBuilder;
        return this;
    }

    public IncubatingApplicationModelResolver setCollectReloadableModules(boolean collectReloadableModules) {
        this.collectReloadableModules = collectReloadableModules;
        return this;
    }

    public IncubatingApplicationModelResolver setDependencyLogging(DependencyLoggingConfig depLogging) {
        this.depLogging = depLogging;
        return this;
    }

    /**
     * In addition to resolving dependencies for the build classpath, also resolve these compile-only dependencies
     * and add them to the application model as {@link DependencyFlags#COMPILE_ONLY}.
     *
     * @param collectCompileOnly compile-only dependencies to add to the model
     * @return self
     */
    public IncubatingApplicationModelResolver setCollectCompileOnly(List<Dependency> collectCompileOnly) {
        this.collectCompileOnly = collectCompileOnly;
        return this;
    }

    public void resolve(CollectRequest collectRtDepsRequest) throws AppModelResolverException {
        this.managedDeps = collectRtDepsRequest.getManagedDependencies();
        // managed dependencies will be a bit augmented with every added extension, so let's load the properties early
        collectPlatformProperties();
        this.managedDeps = managedDeps.isEmpty() ? new ArrayList<>() : managedDeps;

        DependencyNode root = resolveRuntimeDeps(collectRtDepsRequest);
        processRuntimeDeps(root);
        final List<ConditionalDependency> activatedConditionalDeps = activateConditionalDeps();

        // resolve and inject deployment dependency branches for the top (first met) runtime extension nodes
        injectDeployment(activatedConditionalDeps);
        root = normalize(resolver.getSession(), root);
        processDeploymentDeps(root);

        for (var d : appBuilder.getDependencies()) {
            if (!d.isFlagSet(DependencyFlags.RELOADABLE) && !d.isFlagSet(DependencyFlags.VISITED)) {
                clearReloadableFlag(d);
            }
        }

        for (var d : appBuilder.getDependencies()) {
            d.clearFlag(DependencyFlags.VISITED);
            if (d.isFlagSet(DependencyFlags.RELOADABLE)) {
                appBuilder.addReloadableWorkspaceModule(d.getKey());
            }
            appBuilder.addDependency(d);
        }

        collectCompileOnly(collectRtDepsRequest, root);
    }

    private List<ConditionalDependency> activateConditionalDeps() {
        if (conditionalDepsToProcess.isEmpty()) {
            return List.of();
        }
        var activatedConditionalDeps = new ArrayList<ConditionalDependency>();
        boolean checkDependencyConditions = true;
        while (!conditionalDepsToProcess.isEmpty() && checkDependencyConditions) {
            checkDependencyConditions = false;
            var unsatisfiedConditionalDeps = conditionalDepsToProcess;
            conditionalDepsToProcess = new ArrayList<>();
            for (ConditionalDependency cd : unsatisfiedConditionalDeps) {
                if (cd.isSatisfied()) {
                    cd.activate();
                    activatedConditionalDeps.add(cd);
                    // if a dependency was activated, the remaining not satisfied conditions should be checked again
                    checkDependencyConditions = true;
                } else {
                    conditionalDepsToProcess.add(cd);
                }
            }
        }
        return activatedConditionalDeps;
    }

    private void processDeploymentDeps(DependencyNode root) {
        var app = new AppDep(root);
        final ModelResolutionTaskRunner taskRunner = new ModelResolutionTaskRunner();
        app.scheduleChildVisits(taskRunner, AppDep::scheduleDeploymentVisit);
        taskRunner.waitForCompletion();
        appBuilder.getApplicationArtifact().addDependencies(app.allDeps);
        for (var d : app.children) {
            d.addToModel();
        }

        if (depLogging != null) {
            new AppDepLogger().log(app);
        }
    }

    private void injectDeployment(List<ConditionalDependency> activatedConditionalDeps) {
        final ConcurrentLinkedDeque<Runnable> injectQueue = new ConcurrentLinkedDeque<>();
        collectDeploymentDeps(injectQueue);
        if (!activatedConditionalDeps.isEmpty()) {
            collectConditionalDeploymentDeps(activatedConditionalDeps, injectQueue);
        }
        for (var inject : injectQueue) {
            inject.run();
        }
    }

    private void collectConditionalDeploymentDeps(List<ConditionalDependency> activatedConditionalDeps,
            ConcurrentLinkedDeque<Runnable> injectQueue) {
        var taskRunner = new ModelResolutionTaskRunner();
        for (ConditionalDependency cd : activatedConditionalDeps) {
            taskRunner.run(() -> {
                var resolvedDep = appBuilder.getDependency(getKey(cd.conditionalDep.ext.info.deploymentArtifact));
                if (resolvedDep == null) {
                    var extDep = cd.getExtensionDependency();
                    extDep.collectDeploymentDeps();
                    injectQueue.add(() -> extDep.injectDeploymentNode(cd.conditionalDep.ext.getParentDeploymentNode()));
                } else {
                    // if resolvedDep isn't null, it means the deployment artifact is on the runtime classpath
                    // in which case we also clear the reloadable flag on it, in case it's coming from the workspace
                    resolvedDep.clearFlag(DependencyFlags.RELOADABLE);
                }
            });
        }
        taskRunner.waitForCompletion();
    }

    private void collectDeploymentDeps(ConcurrentLinkedDeque<Runnable> injectQueue) {
        var taskRunner = new ModelResolutionTaskRunner();
        for (ExtensionDependency extDep : topExtensionDeps) {
            taskRunner.run(() -> {
                var resolvedDep = appBuilder.getDependency(getKey(extDep.info.deploymentArtifact));
                if (resolvedDep == null) {
                    extDep.collectDeploymentDeps();
                    injectQueue.add(() -> extDep.injectDeploymentNode(null));
                } else {
                    // if resolvedDep isn't null, it means the deployment artifact is on the runtime classpath
                    // in which case we also clear the reloadable flag on it, in case it's coming from the workspace
                    resolvedDep.clearFlag(DependencyFlags.RELOADABLE);
                }
            });
        }
        // non-conditional deployment branches should be added before the activated conditional ones to have consistent
        // dependency graph structures
        taskRunner.waitForCompletion();
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
                if (hasWinner(node)) {
                    continue;
                }
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
            if ("json".equals(extension)
                    && artifact.getArtifactId().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)) {
                platformReleases.addPlatformDescriptor(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getClassifier(), extension,
                        artifact.getVersion());
            } else if ("properties".equals(extension)
                    && artifact.getArtifactId().endsWith(BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX)) {
                platformReleases.addPlatformProperties(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getClassifier(), extension,
                        artifact.getVersion(), resolver.resolve(artifact).getArtifact().getFile().toPath());
            }
        }
        appBuilder.setPlatformImports(platformReleases);
    }

    private void clearReloadableFlag(ResolvedDependencyBuilder dep) {
        final Collection<ArtifactCoords> deps = dep.getDependencies();
        if (deps.isEmpty()) {
            return;
        }
        for (ArtifactCoords coords : deps) {
            final ResolvedDependencyBuilder child = appBuilder.getDependency(coords.getKey());
            if (child == null || child.isFlagSet(DependencyFlags.VISITED)) {
                continue;
            }
            child.setFlags(DependencyFlags.VISITED);
            child.clearFlag(DependencyFlags.RELOADABLE);
            clearReloadableFlag(child);
        }
    }

    private DependencyNode normalize(RepositorySystemSession session, DependencyNode root) throws AppModelResolverException {
        final DependencyGraphTransformationContext context = new SimpleDependencyGraphTransformationContext(session);
        try {
            // resolves version conflicts
            root = new ConflictIdSorter().transformGraph(root, context);
            return session.getDependencyGraphTransformer().transformGraph(root, context);
        } catch (RepositoryException e) {
            throw new AppModelResolverException("Failed to resolve dependency graph conflicts", e);
        }
    }

    private DependencyNode resolveRuntimeDeps(CollectRequest request)
            throws AppModelResolverException {
        boolean verbose = true; //Boolean.getBoolean("quarkus.bootstrap.verbose-model-resolver");
        if (verbose) {
            var session = resolver.getSession();
            final DefaultRepositorySystemSession mutableSession = new DefaultRepositorySystemSession(resolver.getSession());
            mutableSession.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
            mutableSession.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
            session = mutableSession;

            var ctx = new BootstrapMavenContext(BootstrapMavenContext.config()
                    .setRepositorySystem(resolver.getSystem())
                    .setRepositorySystemSession(session)
                    .setRemoteRepositories(resolver.getRepositories())
                    .setRemoteRepositoryManager(resolver.getRemoteRepositoryManager())
                    .setCurrentProject(resolver.getMavenContext().getCurrentProject())
                    .setWorkspaceDiscovery(collectReloadableModules));
            resolver = new MavenArtifactResolver(ctx);
        }
        try {
            return resolver.getSystem().collectDependencies(resolver.getSession(), request).getRoot();
        } catch (DependencyCollectionException e) {
            final Artifact a = request.getRoot() == null ? request.getRootArtifact() : request.getRoot().getArtifact();
            throw new BootstrapMavenException("Failed to resolve dependencies for " + a, e);
        }
    }

    private boolean isRuntimeArtifact(ArtifactKey key) {
        final ResolvedDependencyBuilder dep = appBuilder.getDependency(key);
        return dep != null && dep.isFlagSet(DependencyFlags.RUNTIME_CP);
    }

    private void processRuntimeDeps(DependencyNode root) {
        final AppDep appRoot = new AppDep(root);
        appRoot.walkingFlags = COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS;
        if (collectReloadableModules) {
            appRoot.walkingFlags |= COLLECT_RELOADABLE_MODULES;
        }

        final ModelResolutionTaskRunner taskRunner = new ModelResolutionTaskRunner();
        appRoot.scheduleChildVisits(taskRunner, AppDep::scheduleRuntimeVisit);
        taskRunner.waitForCompletion();
        appBuilder.getApplicationArtifact().addDependencies(appRoot.allDeps);
        appRoot.setChildFlags();
    }

    private class AppDep {
        final AppDep parent;
        final DependencyNode node;
        ExtensionDependency ext;
        byte walkingFlags;
        ResolvedDependencyBuilder resolvedDep;
        final List<AppDep> children;
        final List<ArtifactCoords> allDeps;

        AppDep(DependencyNode node) {
            this.parent = null;
            this.node = node;
            this.children = new ArrayList<>(node.getChildren().size());
            this.allDeps = new ArrayList<>(node.getChildren().size());
        }

        AppDep(AppDep parent, DependencyNode node) {
            this.parent = parent;
            this.node = node;
            this.children = new ArrayList<>(node.getChildren().size());
            this.allDeps = new ArrayList<>(node.getChildren().size());
        }

        void addToModel() {
            for (var child : children) {
                child.addToModel();
            }
            // this node is added after its children to stay compatible with the legacy impl
            if (resolvedDep != null) {
                resolvedDep.addDependencies(allDeps);
                appBuilder.addDependency(resolvedDep);
            }
        }

        void scheduleDeploymentVisit(ModelResolutionTaskRunner taskRunner) {
            taskRunner.run(this::visitDeploymentDependency);
            scheduleChildVisits(taskRunner, AppDep::scheduleDeploymentVisit);
        }

        void visitDeploymentDependency() {
            if (!appBuilder.hasDependency(getKey(node.getArtifact()))) {
                try {
                    resolvedDep = newDependencyBuilder(node, resolver)
                            .setFlags(DependencyFlags.DEPLOYMENT_CP);
                } catch (BootstrapMavenException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        void scheduleRuntimeVisit(ModelResolutionTaskRunner taskRunner) {
            taskRunner.run(this::visitRuntimeDependency);
            scheduleChildVisits(taskRunner, AppDep::scheduleRuntimeVisit);
        }

        void visitRuntimeDependency() {
            Artifact artifact = node.getArtifact();
            final ArtifactKey key = getKey(artifact);
            if (resolvedDep == null) {
                resolvedDep = appBuilder.getDependency(key);
            }

            // in case it was relocated it might not survive conflict resolution in the deployment graph
            if (!node.getRelocations().isEmpty()) {
                ((DefaultDependencyNode) node).setRelocations(List.of());
            }
            try {
                var ext = getExtensionDependencyOrNull();
                if (resolvedDep == null) {
                    WorkspaceModule module = null;
                    if (resolver.getProjectModuleResolver() != null) {
                        module = resolver.getProjectModuleResolver().getProjectModule(artifact.getGroupId(),
                                artifact.getArtifactId(), artifact.getVersion());
                    }
                    resolvedDep = DependencyUtils.toAppArtifact(getResolvedArtifact(), module)
                            .setOptional(node.getDependency().isOptional())
                            .setScope(node.getDependency().getScope())
                            .setRuntimeCp()
                            .setDeploymentCp();
                    if (JavaScopes.PROVIDED.equals(resolvedDep.getScope())) {
                        resolvedDep.setFlags(DependencyFlags.COMPILE_ONLY);
                    }
                    if (ext != null) {
                        resolvedDep.setRuntimeExtensionArtifact();
                        collectConditionalDependencies();
                    }
                }
            } catch (DeploymentInjectionException e) {
                throw e;
            } catch (Exception t) {
                throw new DeploymentInjectionException("Failed to inject extension deployment dependencies", t);
            }
        }

        void scheduleChildVisits(ModelResolutionTaskRunner taskRunner,
                BiConsumer<AppDep, ModelResolutionTaskRunner> childVisitor) {
            var childNodes = node.getChildren();
            List<DependencyNode> filtered = null;
            for (int i = 0; i < childNodes.size(); ++i) {
                var childNode = childNodes.get(i);
                var winner = getWinner(childNode);
                if (winner == null) {
                    allDeps.add(getCoords(childNode.getArtifact()));
                    var child = new AppDep(this, childNode);
                    children.add(child);
                    if (filtered != null) {
                        filtered.add(childNode);
                    }
                } else {
                    allDeps.add(getCoords(winner.getArtifact()));
                    if (filtered == null) {
                        filtered = new ArrayList<>(childNodes.size());
                        for (int j = 0; j < i; ++j) {
                            filtered.add(childNodes.get(j));
                        }
                    }
                }
            }
            if (filtered != null) {
                node.setChildren(filtered);
            }
            for (var child : children) {
                childVisitor.accept(child, taskRunner);
            }
        }

        void setChildFlags() {
            for (var c : children) {
                c.setFlags(walkingFlags);
            }
        }

        void setFlags(byte walkingFlags) {

            resolvedDep.addDependencies(allDeps);
            if (ext != null) {
                var parentExtDep = parent;
                while (parentExtDep != null) {
                    if (parentExtDep.ext != null) {
                        parentExtDep.ext.addExtensionDependency(ext);
                        break;
                    }
                    parentExtDep = parentExtDep.parent;
                }
                ext.info.ensureActivated();
            }

            var existingDep = appBuilder.getDependency(resolvedDep.getKey());
            if (existingDep == null) {
                appBuilder.addDependency(resolvedDep);
                if (ext != null) {
                    managedDeps.add(new Dependency(ext.info.deploymentArtifact, JavaScopes.COMPILE));
                }
            } else if (existingDep != resolvedDep) {
                throw new IllegalStateException(node.getArtifact() + " is already in the model");
            }
            this.walkingFlags = walkingFlags;

            resolvedDep.setDirect(isWalkingFlagOn(COLLECT_DIRECT_DEPS));
            if (ext != null && isWalkingFlagOn(COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
                resolvedDep.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                clearWalkingFlag(COLLECT_TOP_EXTENSION_RUNTIME_NODES);
                topExtensionDeps.add(ext);
            }
            if (isWalkingFlagOn(COLLECT_RELOADABLE_MODULES)) {
                if (resolvedDep.getWorkspaceModule() != null
                        && !resolvedDep.isFlagSet(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {
                    resolvedDep.setReloadable();
                } else {
                    clearWalkingFlag(COLLECT_RELOADABLE_MODULES);
                }
            }

            clearWalkingFlag(COLLECT_DIRECT_DEPS);
            setChildFlags();
        }

        private ExtensionDependency getExtensionDependencyOrNull()
                throws BootstrapDependencyProcessingException {
            if (ext != null) {
                return ext;
            }
            ext = ExtensionDependency.get(node);
            if (ext == null) {
                final ExtensionInfo extInfo = getExtensionInfoOrNull(node.getArtifact(), node.getRepositories());
                if (extInfo != null) {
                    ext = new ExtensionDependency(extInfo, node, collectExclusions());
                }
            }
            return ext;
        }

        private Collection<Exclusion> collectExclusions() {
            if (parent == null) {
                return List.of();
            }
            Collection<Exclusion> exclusions = null;
            var next = this;
            while (next != null) {
                if (next.ext != null) {
                    if (exclusions == null) {
                        return next.ext.exclusions;
                    }
                    exclusions.addAll(next.ext.exclusions);
                    return exclusions;
                }
                var nextExcl = next.node.getDependency() == null ? null : next.node.getDependency().getExclusions();
                if (nextExcl != null && !nextExcl.isEmpty()) {
                    if (exclusions == null) {
                        exclusions = new ArrayList<>(nextExcl);
                    }
                }
                next = next.parent;
            }
            return exclusions == null ? List.of() : exclusions;
        }

        Artifact getResolvedArtifact() {
            var result = node.getArtifact();
            if (result.getFile() == null) {
                result = resolve(result, node.getRepositories());
                node.setArtifact(result);
            }
            return result;
        }

        private boolean isWalkingFlagOn(byte flag) {
            return (walkingFlags & flag) > 0;
        }

        private void clearWalkingFlag(byte flag) {
            if ((walkingFlags & flag) > 0) {
                walkingFlags ^= flag;
            }
        }

        private void collectConditionalDependencies()
                throws BootstrapDependencyProcessingException {
            if (ext.info.conditionalDeps.length == 0 || ext.conditionalDepsQueued) {
                return;
            }
            ext.conditionalDepsQueued = true;

            final DependencySelector selector = ext.exclusions == null ? null
                    : new ExclusionDependencySelector(ext.exclusions);
            for (Artifact conditionalArtifact : ext.info.conditionalDeps) {
                if (selector != null && !selector.selectDependency(new Dependency(conditionalArtifact, JavaScopes.RUNTIME))) {
                    continue;
                }
                final ExtensionInfo conditionalInfo = getExtensionInfoOrNull(conditionalArtifact,
                        ext.runtimeNode.getRepositories());
                if (conditionalInfo == null) {
                    log.warn(ext.info.runtimeArtifact + " declares a conditional dependency on " + conditionalArtifact
                            + " that is not a Quarkus extension and will be ignored");
                    continue;
                }
                if (conditionalInfo.activated) {
                    continue;
                }
                final ConditionalDependency conditionalDep = new ConditionalDependency(conditionalInfo, this);
                conditionalDepsToProcess.add(conditionalDep);
                conditionalDep.conditionalDep.collectConditionalDependencies();
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
            return ext == EXT_INFO_NONE ? null : ext;
        }
        artifact = resolve(artifact, repos);
        final Path path = artifact.getFile().toPath();
        final Properties descriptor = PathTree.ofDirectoryOrArchive(path).apply(BootstrapConstants.DESCRIPTOR_PATH, visit -> {
            if (visit == null) {
                return null;
            }
            try {
                return readDescriptor(visit.getPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        if (descriptor == null) {
            allExtensions.put(extKey, EXT_INFO_NONE);
            return null;
        }
        ext = new ExtensionInfo(artifact, descriptor);
        allExtensions.put(extKey, ext);
        return ext;
    }

    private DependencyNode collectDependencies(Artifact artifact, Collection<Exclusion> exclusions,
            List<RemoteRepository> repos) {
        DependencyNode root;
        try {
            root = resolver.getSystem()
                    .collectDependencies(resolver.getSession(), getCollectRequest(artifact, exclusions, repos))
                    .getRoot();
        } catch (DependencyCollectionException e) {
            throw new DeploymentInjectionException("Failed to collect dependencies for " + artifact, e);
        }
        if (root.getChildren().size() != 1) {
            throw new DeploymentInjectionException("Only one child expected but got " + root.getChildren());
        }
        return root.getChildren().get(0);
    }

    private CollectRequest getCollectRequest(Artifact artifact, Collection<Exclusion> exclusions,
            List<RemoteRepository> repos) {
        final ArtifactDescriptorResult descr;
        try {
            descr = resolver.resolveDescriptor(artifact, repos);
        } catch (BootstrapMavenException e) {
            throw new DeploymentInjectionException("Failed to resolve descriptor for " + artifact, e);
        }
        final List<Dependency> allConstraints = new ArrayList<>(
                managedDeps.size() + descr.getManagedDependencies().size());
        allConstraints.addAll(managedDeps);
        allConstraints.addAll(descr.getManagedDependencies());
        return new CollectRequest()
                .setManagedDependencies(allConstraints)
                .setRepositories(repos)
                .setRootArtifact(artifact)
                .setDependencies(List.of(new Dependency(artifact, JavaScopes.COMPILE, false, exclusions)));
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

    private static Properties readDescriptor(Path path) throws IOException {
        final Properties rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            rtProps.load(reader);
        }
        return rtProps;
    }

    private class ExtensionInfo {

        final Artifact runtimeArtifact;
        final Properties props;
        final Artifact deploymentArtifact;
        final Artifact[] conditionalDeps;
        final ArtifactKey[] dependencyCondition;
        boolean activated;

        private ExtensionInfo() {
            runtimeArtifact = null;
            props = null;
            deploymentArtifact = null;
            conditionalDeps = null;
            dependencyCondition = null;
        }

        ExtensionInfo(Artifact runtimeArtifact, Properties props) throws BootstrapDependencyProcessingException {
            this.runtimeArtifact = runtimeArtifact;
            this.props = props;

            String value = props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            if (value == null) {
                throw new BootstrapDependencyProcessingException("Extension descriptor from " + runtimeArtifact
                        + " does not include " + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            }
            Artifact deploymentArtifact = toArtifact(value);
            if (deploymentArtifact.getVersion() == null || deploymentArtifact.getVersion().isEmpty()) {
                deploymentArtifact = deploymentArtifact.setVersion(runtimeArtifact.getVersion());
            }
            this.deploymentArtifact = deploymentArtifact;

            value = props.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES);
            if (value != null) {
                final String[] deps = BootstrapUtils.splitByWhitespace(value);
                conditionalDeps = new Artifact[deps.length];
                for (int i = 0; i < deps.length; ++i) {
                    try {
                        conditionalDeps[i] = toArtifact(deps[i]);
                    } catch (Exception e) {
                        throw new BootstrapDependencyProcessingException(
                                "Failed to parse conditional dependencies configuration of " + runtimeArtifact, e);
                    }
                }
            } else {
                conditionalDeps = NO_ARTIFACTS;
            }

            dependencyCondition = BootstrapUtils
                    .parseDependencyCondition(props.getProperty(BootstrapConstants.DEPENDENCY_CONDITION));
        }

        void ensureActivated() {
            if (activated) {
                return;
            }
            activated = true;
            appBuilder.handleExtensionProperties(props, runtimeArtifact.toString());

            final String providesCapabilities = props.getProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES);
            final String requiresCapabilities = props.getProperty(BootstrapConstants.PROP_REQUIRES_CAPABILITIES);
            if (providesCapabilities != null || requiresCapabilities != null) {
                appBuilder.addExtensionCapabilities(
                        CapabilityContract.of(toCompactCoords(runtimeArtifact), providesCapabilities, requiresCapabilities));
            }
        }
    }

    private class ExtensionDependency {

        static ExtensionDependency get(DependencyNode node) {
            return (ExtensionDependency) node.getData().get(QUARKUS_EXTENSION_DEPENDENCY);
        }

        final ExtensionInfo info;
        final DependencyNode runtimeNode;
        final Collection<Exclusion> exclusions;
        boolean conditionalDepsQueued;
        private List<ExtensionDependency> extDeps;
        private DependencyNode deploymentNode;
        private DependencyNode parentNode;

        ExtensionDependency(ExtensionInfo info, DependencyNode node, Collection<Exclusion> exclusions) {
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

        DependencyNode getParentDeploymentNode() {
            if (parentNode == null) {
                return null;
            }
            var ext = ExtensionDependency.get(parentNode);
            if (ext == null) {
                return null;
            }
            return ext.deploymentNode == null ? ext.parentNode : ext.deploymentNode;
        }

        void addExtensionDependency(ExtensionDependency dep) {
            if (extDeps == null) {
                extDeps = new ArrayList<>();
            }
            extDeps.add(dep);
        }

        private void collectDeploymentDeps()
                throws BootstrapDependencyProcessingException {
            log.debugf("Collecting dependencies of %s", info.deploymentArtifact);
            deploymentNode = collectDependencies(info.deploymentArtifact, exclusions, runtimeNode.getRepositories());
            if (deploymentNode.getChildren().isEmpty()) {
                throw new BootstrapDependencyProcessingException(
                        "Failed to collect dependencies of " + deploymentNode.getArtifact()
                                + ": either its POM could not be resolved from the available Maven repositories "
                                + "or the artifact does not have any dependencies while at least a dependency on the runtime artifact "
                                + info.runtimeArtifact + " is expected");
            }
            if (!replaceDirectDepBranch(deploymentNode, true)) {
                throw new BootstrapDependencyProcessingException(
                        "Quarkus extension deployment artifact " + deploymentNode.getArtifact()
                                + " does not appear to depend on the corresponding runtime artifact "
                                + info.runtimeArtifact);
            }
        }

        private void injectDeploymentNode(DependencyNode parentDeploymentNode) {
            if (parentDeploymentNode == null) {
                runtimeNode.setData(QUARKUS_RUNTIME_ARTIFACT, runtimeNode.getArtifact());
                runtimeNode.setArtifact(deploymentNode.getArtifact());
                runtimeNode.setChildren(deploymentNode.getChildren());
            } else {
                parentDeploymentNode.getChildren().add(deploymentNode);
            }
        }

        private boolean replaceDirectDepBranch(DependencyNode parentNode, boolean replaceRuntimeNode) {
            int i = 0;
            DependencyNode inserted = null;
            var childNodes = parentNode.getChildren();
            while (i < childNodes.size()) {
                var node = childNodes.get(i);
                final Artifact a = node.getArtifact();
                if (a != null && !hasWinner(node) && isSameKey(info.runtimeArtifact, a)) {
                    // we are not comparing the version in the above condition because the runtime version
                    // may appear to be different from the deployment one and that's ok
                    // e.g. the version of the runtime artifact could be managed by a BOM
                    // but overridden by the user in the project config. The way the deployment deps
                    // are resolved here, the deployment version of the runtime artifact will be the one from the BOM.
                    if (replaceRuntimeNode) {
                        inserted = new DefaultDependencyNode(runtimeNode);
                        inserted.setChildren(runtimeNode.getChildren());
                        childNodes.set(i, inserted);
                    } else {
                        inserted = runtimeNode;
                    }
                    if (this.deploymentNode == null && this.parentNode == null) {
                        this.parentNode = parentNode;
                    }
                    break;
                }
                ++i;
            }
            if (inserted == null) {
                return false;
            }

            if (extDeps != null) {
                var depQueue = new ArrayList<>(childNodes);
                var exts = new ArrayList<>(extDeps);
                for (int j = 0; j < depQueue.size(); ++j) {
                    var depNode = depQueue.get(j);
                    if (hasWinner(depNode)) {
                        continue;
                    }
                    for (int k = 0; k < exts.size(); ++k) {
                        if (exts.get(k).replaceDirectDepBranch(depNode, replaceRuntimeNode && depNode != inserted)) {
                            exts.remove(k);
                            break;
                        }
                    }
                    if (exts.isEmpty()) {
                        break;
                    }
                    depQueue.addAll(depNode.getChildren());
                }
            }

            return true;
        }
    }

    private class ConditionalDependency {

        final AppDep conditionalDep;
        private boolean activated;

        private ConditionalDependency(ExtensionInfo info, AppDep parent) {
            final DefaultDependencyNode rtNode = new DefaultDependencyNode(
                    new Dependency(info.runtimeArtifact, JavaScopes.COMPILE));
            rtNode.setVersion(new BootstrapArtifactVersion(info.runtimeArtifact.getVersion()));
            rtNode.setVersionConstraint(new BootstrapArtifactVersionConstraint(
                    new BootstrapArtifactVersion(info.runtimeArtifact.getVersion())));
            rtNode.setRepositories(parent.ext.runtimeNode.getRepositories());

            conditionalDep = new AppDep(parent, rtNode);
            conditionalDep.ext = new ExtensionDependency(info, rtNode, parent.ext.exclusions);
        }

        ExtensionDependency getExtensionDependency() {
            return conditionalDep.ext;
        }

        void activate() {
            if (activated) {
                return;
            }
            activated = true;
            final ExtensionDependency extDep = getExtensionDependency();
            final DependencyNode originalNode = collectDependencies(conditionalDep.ext.info.runtimeArtifact, extDep.exclusions,
                    extDep.runtimeNode.getRepositories());
            final DefaultDependencyNode rtNode = (DefaultDependencyNode) extDep.runtimeNode;
            rtNode.setRepositories(originalNode.getRepositories());
            // if this node has conditional dependencies on its own, they may have been activated by this time
            // in which case they would be included into its children
            List<DependencyNode> currentChildren = rtNode.getChildren();
            if (currentChildren == null || currentChildren.isEmpty()) {
                rtNode.setChildren(originalNode.getChildren());
            } else {
                currentChildren.addAll(originalNode.getChildren());
            }
            conditionalDep.walkingFlags = COLLECT_DIRECT_DEPS;
            if (collectReloadableModules) {
                conditionalDep.walkingFlags |= COLLECT_RELOADABLE_MODULES;
            }
            var taskRunner = new ModelResolutionTaskRunner();
            conditionalDep.scheduleRuntimeVisit(taskRunner);
            taskRunner.waitForCompletion();
            conditionalDep.setFlags(conditionalDep.walkingFlags);
            if (conditionalDep.parent.resolvedDep == null) {
                conditionalDep.parent.allDeps.add(conditionalDep.resolvedDep.getArtifactCoords());
            } else {
                conditionalDep.parent.resolvedDep.addDependency(conditionalDep.resolvedDep.getArtifactCoords());
            }
            conditionalDep.parent.ext.runtimeNode.getChildren().add(rtNode);
        }

        boolean isSatisfied() {
            if (conditionalDep.ext.info.dependencyCondition == null) {
                return true;
            }
            for (ArtifactKey key : conditionalDep.ext.info.dependencyCondition) {
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

    private static String toCompactCoords(Artifact a) {
        final StringBuilder b = new StringBuilder();
        b.append(a.getGroupId()).append(':').append(a.getArtifactId()).append(':');
        if (!a.getClassifier().isEmpty()) {
            b.append(a.getClassifier()).append(':');
        }
        if (!ArtifactCoords.TYPE_JAR.equals(a.getExtension())) {
            b.append(a.getExtension()).append(':');
        }
        b.append(a.getVersion());
        return b.toString();
    }

    private class AppDepLogger {

        final List<Boolean> depth = new ArrayList<>();

        private AppDepLogger() {
        }

        void log(AppDep dep) {
            logInternal(dep);
            final int childrenTotal = dep.node.getChildren().size();
            if (childrenTotal > 0) {
                if (childrenTotal == 1) {
                    depth.add(false);
                    log(dep.children.get(0));
                } else {
                    depth.add(true);
                    int i = 0;
                    while (i < childrenTotal) {
                        log(dep.children.get(i++));
                        if (i == childrenTotal - 1) {
                            depth.set(depth.size() - 1, false);
                        }
                    }
                }
                depth.remove(depth.size() - 1);
            }
        }

        private void logInternal(AppDep dep) {
            var buf = new StringBuilder();
            if (!depth.isEmpty()) {
                for (int i = 0; i < depth.size() - 1; ++i) {
                    if (depth.get(i)) {
                        //buf.append("|  ");
                        buf.append('\u2502').append("  ");
                    } else {
                        buf.append("   ");
                    }
                }
                if (depth.get(depth.size() - 1)) {
                    //buf.append("|- ");
                    buf.append('\u251c').append('\u2500').append(' ');
                } else {
                    //buf.append("\\- ");
                    buf.append('\u2514').append('\u2500').append(' ');
                }
            }
            var resolvedDep = getResolvedDependency(getKey(dep.node.getArtifact()));
            buf.append(resolvedDep.toCompactCoords());
            if (!depth.isEmpty()) {
                appendFlags(buf, resolvedDep);
            }
            depLogging.getMessageConsumer().accept(buf.toString());

            if (depLogging.isGraph()) {
                var deps = resolvedDep.getDependencies();
                if (!deps.isEmpty() && deps.size() != dep.children.size()) {
                    final Map<ArtifactCoords, Object> versions = new HashMap<>(dep.children.size());
                    for (var c : dep.children) {
                        versions.put(getCoords(c.node.getArtifact()), null);
                    }
                    var list = new ArrayList<String>(deps.size() - dep.children.size());
                    for (var coords : deps) {
                        if (!versions.containsKey(coords)) {
                            var childDep = getResolvedDependency(coords.getKey());
                            var sb = new StringBuilder().append(childDep.toCompactCoords());
                            appendFlags(sb, childDep);
                            list.add(sb.append(" [+]").toString());
                        }
                    }
                    Collections.sort(list);
                    for (int j = 0; j < list.size(); ++j) {
                        buf = new StringBuilder();
                        if (!depth.isEmpty()) {
                            for (int i = 0; i < depth.size() - 1; ++i) {
                                if (depth.get(i)) {
                                    //buf.append("|  ");
                                    buf.append('\u2502').append("  ");
                                } else {
                                    buf.append("   ");
                                }
                            }
                            if (depth.get(depth.size() - 1)) {
                                //buf.append("|  ");
                                buf.append('\u2502').append("  ");
                            } else {
                                buf.append("   ");
                            }
                        }

                        if (j < list.size() - 1) {
                            //buf.append("|- ");
                            buf.append('\u251c').append('\u2500').append(' ');
                        } else if (dep.children.isEmpty()) {
                            //buf.append("\\- ");
                            buf.append('\u2514').append('\u2500').append(' ');
                        } else {
                            //buf.append("|- ");
                            buf.append('\u251c').append('\u2500').append(' ');
                        }
                        buf.append(list.get(j));
                        depLogging.getMessageConsumer().accept(buf.toString());
                    }
                }
            }
        }

        private void appendFlags(StringBuilder sb, ResolvedDependency d) {
            sb.append(" (").append(d.getScope());
            if (d.isFlagSet(DependencyFlags.OPTIONAL)) {
                sb.append(" optional");
            }
            if (depLogging.isVerbose()) {
                if (d.isFlagSet(DependencyFlags.RUNTIME_CP)) {
                    sb.append(", runtime classpath");
                } else {
                    sb.append(", build-time classpath");
                }
                if (d.isFlagSet(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {
                    sb.append(", extension");
                }
                if (d.isFlagSet(DependencyFlags.RELOADABLE)) {
                    sb.append(", reloadable");
                }
            }
            sb.append(')');
        }

        private ResolvedDependencyBuilder getResolvedDependency(ArtifactKey key) {
            var resolvedDep = appBuilder.getDependency(key);
            if (resolvedDep == null) {
                if (appBuilder.getApplicationArtifact().getKey().equals(key)) {
                    return appBuilder.getApplicationArtifact();
                }
                throw new IllegalArgumentException("Failed to locate " + key + " among application dependencies");
            }
            return resolvedDep;
        }
    }
}
