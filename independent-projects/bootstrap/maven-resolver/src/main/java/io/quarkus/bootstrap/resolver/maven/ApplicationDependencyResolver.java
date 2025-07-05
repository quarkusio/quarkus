package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getCoords;
import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.getWinner;
import static io.quarkus.bootstrap.util.DependencyUtils.hasWinner;
import static io.quarkus.bootstrap.util.DependencyUtils.newDependencyBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
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
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;

public class ApplicationDependencyResolver {

    private static final Logger log = Logger.getLogger(ApplicationDependencyResolver.class);

    private static final String QUARKUS_RUNTIME_ARTIFACT = "quarkus.runtime";
    private static final String QUARKUS_EXTENSION_DEPENDENCY = "quarkus.ext";

    /* @formatter:off */
    private static final byte COLLECT_TOP_EXTENSION_RUNTIME_NODES = 0b0001;
    private static final byte COLLECT_DIRECT_DEPS =                 0b0010;
    private static final byte COLLECT_RELOADABLE_MODULES =          0b0100;
    private static final byte COLLECT_DEPLOYMENT_INJECTION_POINTS = 0b1000;
    /* @formatter:on */

    /**
     * Whether to use a blocking or non-blocking dependency resolution and processing task runner
     */
    private static final boolean BLOCKING_TASK_RUNNER = Boolean.getBoolean("quarkus.bootstrap.blocking-task-runner");

    public static ApplicationDependencyResolver newInstance() {
        return new ApplicationDependencyResolver();
    }

    /**
     * Returns a task runner.
     *
     * @return task runner
     */
    private static ModelResolutionTaskRunner getTaskRunner() {
        return BLOCKING_TASK_RUNNER ? ModelResolutionTaskRunner.getBlockingTaskRunner()
                : ModelResolutionTaskRunner.getNonBlockingTaskRunner();
    }

    private final ExtensionInfo EXT_INFO_NONE = new ExtensionInfo();

    private List<AppDep> deploymentInjectionPoints = new ArrayList<>();
    private final Map<ArtifactKey, ExtensionInfo> allExtensions = new ConcurrentHashMap<>();
    private Collection<ConditionalDependency> conditionalDepsToProcess = new ConcurrentLinkedDeque<>();

    private MavenArtifactResolver resolver;
    private List<Dependency> managedDeps;
    private ApplicationModelBuilder appBuilder;
    private boolean collectReloadableModules;
    private DependencyLoggingConfig depLogging;
    private List<Dependency> collectCompileOnly;
    private boolean runtimeModelOnly;
    private boolean devMode;

    /**
     * Maven artifact resolver that should be used to resolve application dependencies
     *
     * @param resolver Maven artifact resolver
     * @return self
     */
    public ApplicationDependencyResolver setArtifactResolver(MavenArtifactResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    /**
     * Application model builder to add the resolved dependencies to.
     *
     * @param appBuilder application model builder
     * @return self
     */
    public ApplicationDependencyResolver setApplicationModelBuilder(ApplicationModelBuilder appBuilder) {
        this.appBuilder = appBuilder;
        return this;
    }

    /**
     * Whether to indicate which resolved dependencies are reloadable.
     *
     * @param collectReloadableModules whether indicate which resolved dependencies are reloadable
     * @return self
     */
    public ApplicationDependencyResolver setCollectReloadableModules(boolean collectReloadableModules) {
        this.collectReloadableModules = collectReloadableModules;
        return this;
    }

    /**
     * Dependency logging configuration. For example to log the resolved dependency tree.
     *
     * @param depLogging dependency logging configuration
     * @return self
     */
    public ApplicationDependencyResolver setDependencyLogging(DependencyLoggingConfig depLogging) {
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
    public ApplicationDependencyResolver setCollectCompileOnly(List<Dependency> collectCompileOnly) {
        this.collectCompileOnly = collectCompileOnly;
        return this;
    }

    /**
     * Whether to limit the resulting {@link io.quarkus.bootstrap.model.ApplicationModel} to the runtime dependencies.
     *
     * @param runtimeModelOnly whether to limit the resulting application model to the runtime dependencies
     * @return self
     */
    public ApplicationDependencyResolver setRuntimeModelOnly(boolean runtimeModelOnly) {
        this.runtimeModelOnly = runtimeModelOnly;
        return this;
    }

    /**
     * Whether an application model is resolved for dev mode
     *
     * @param devMode whether an application model is resolved for dev mode
     * @return self
     */
    public ApplicationDependencyResolver setDevMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    /**
     * Resolves application dependencies and adds the to the application model builder.
     *
     * @param collectRtDepsRequest request to collect runtime dependencies
     * @throws AppModelResolverException in case of a failure
     */
    public void resolve(CollectRequest collectRtDepsRequest) throws AppModelResolverException {
        this.managedDeps = collectRtDepsRequest.getManagedDependencies();
        // managed dependencies will be a bit augmented with every added extension, so let's load the properties early
        collectPlatformProperties();
        this.managedDeps = managedDeps.isEmpty() ? new ArrayList<>() : managedDeps;

        DependencyNode root = resolveRuntimeDeps(collectRtDepsRequest);
        processRuntimeDeps(root);
        activateConditionalDeps();
        // resolve and inject deployment dependency branches for the top (first met) runtime extension nodes
        if (!runtimeModelOnly) {
            injectDeploymentDeps();
        }
        DependencyTreeConflictResolver.resolveConflicts(root);
        populateModelBuilder(root);

        // clear the reloadable flags
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
            if (!runtimeModelOnly) {
                d.setFlags(DependencyFlags.DEPLOYMENT_CP);
            }
        }

        if (!runtimeModelOnly) {
            collectCompileOnly(collectRtDepsRequest, root);
        }
    }

    /**
     * Activates satisfied conditional dependencies
     */
    private void activateConditionalDeps() {
        if (conditionalDepsToProcess.isEmpty()) {
            return;
        }
        boolean checkDependencyConditions = true;
        while (!conditionalDepsToProcess.isEmpty() && checkDependencyConditions) {
            checkDependencyConditions = false;
            var unsatisfiedConditionalDeps = conditionalDepsToProcess;
            conditionalDepsToProcess = new ConcurrentLinkedDeque<>();
            for (ConditionalDependency cd : unsatisfiedConditionalDeps) {
                if (cd.isSatisfied()) {
                    cd.activate();
                    // if a dependency was activated, the remaining not satisfied conditions should be checked again
                    checkDependencyConditions = true;
                } else {
                    conditionalDepsToProcess.add(cd);
                }
            }
        }
        conditionalDepsToProcess = List.of();
    }

    /**
     * Initializes resolved dependencies that haven't been initialized and adds them to the application model builder.
     *
     * @param root the root node of the dependency tree
     */
    private void populateModelBuilder(DependencyNode root) {
        var app = new AppDep(root);
        initMissingDependencies(app);
        appBuilder.getApplicationArtifact().addDependencies(app.allDeps);
        for (var d : app.children) {
            d.addToModel();
        }
        if (depLogging != null) {
            new AppDepLogger().log(app);
        }
    }

    /**
     * Initializes dependencies that haven't been initialized yet.
     *
     * @param app the root of the application
     */
    private void initMissingDependencies(AppDep app) {
        final ModelResolutionTaskRunner taskRunner = getTaskRunner();
        app.scheduleChildVisits(taskRunner, AppDep::initMissingDependencies);
        taskRunner.waitForCompletion();
    }

    /**
     * Collects and injects deployment dependencies into the application dependency graph
     */
    private void injectDeploymentDeps() {
        for (var dep : collectDeploymentDeps()) {
            dep.injectDeploymentDependency();
        }
    }

    private Collection<AppDep> collectDeploymentDeps() {
        final ConcurrentLinkedDeque<AppDep> injectQueue = new ConcurrentLinkedDeque<>();
        var taskRunner = deploymentInjectionPoints.size() == 1 ? ModelResolutionTaskRunner.getBlockingTaskRunner()
                : getTaskRunner();
        for (AppDep extDep : deploymentInjectionPoints) {
            extDep.scheduleCollectDeploymentDeps(taskRunner, injectQueue);
        }
        deploymentInjectionPoints = List.of();
        taskRunner.waitForCompletion();
        return injectQueue;
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

    /**
     * Collects platform release information and platform build properties by looking for platform properties
     * artifacts among the dependency version constraints of the project (it's not a direct dependency).
     *
     * @throws AppModelResolverException in case a properties artifact could not be resolved
     */
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

    /**
     * Resolves a project's runtime dependencies. This is the first step in the Quarkus application model resolution.
     * These dependencies do not include Quarkus conditional dependencies.
     *
     * @param request collect dependencies request
     * @return the root of the resolved dependency tree
     * @throws AppModelResolverException in case dependencies could not be resolved
     */
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
                    // no need to discover the workspace in case the current project isn't available
                    .setWorkspaceDiscovery(resolver.getMavenContext().getCurrentProject() != null));
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
        visitRuntimeDeps(appRoot);
        appBuilder.getApplicationArtifact().addDependencies(appRoot.allDeps);
        appRoot.setChildFlags((byte) (COLLECT_TOP_EXTENSION_RUNTIME_NODES
                | COLLECT_DIRECT_DEPS
                | COLLECT_DEPLOYMENT_INJECTION_POINTS
                | (collectReloadableModules ? COLLECT_RELOADABLE_MODULES : 0)));
    }

    private void visitRuntimeDeps(AppDep appRoot) {
        final ModelResolutionTaskRunner taskRunner = getTaskRunner();
        appRoot.scheduleChildVisits(taskRunner, AppDep::scheduleRuntimeVisit);
        taskRunner.waitForCompletion();
    }

    private class AppDep {
        final AppDep parent;
        final DependencyNode node;
        ExtensionDependency ext;
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

        /**
         * Adds this dependency and its dependencies to the application model builder
         */
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

        /**
         * Checks whether this dependency and its dependencies are present in the application model builder and if not
         * adds them.
         *
         * @param taskRunner task runner
         */
        void initMissingDependencies(ModelResolutionTaskRunner taskRunner) {
            if (resolvedDep == null && !appBuilder.hasDependency(getKey(node.getArtifact()))) {
                taskRunner.run(this::initResolvedDependency);
            }
            scheduleChildVisits(taskRunner, AppDep::initMissingDependencies);
        }

        /**
         * Creates a dependency (resolving the artifact if necessary) that will be later added to the application model.
         */
        void initResolvedDependency() {
            try {
                resolvedDep = newDependencyBuilder(node, resolver);
            } catch (BootstrapMavenException e) {
                throw new RuntimeException(e);
            }
        }

        void scheduleRuntimeVisit(ModelResolutionTaskRunner taskRunner) {
            taskRunner.run(this::visitRuntimeDependency);
            scheduleChildVisits(taskRunner, AppDep::scheduleRuntimeVisit);
        }

        void visitRuntimeDependency() {
            final Artifact artifact = node.getArtifact();
            if (resolvedDep == null) {
                resolvedDep = appBuilder.getDependency(getKey(artifact));
            }

            // in case it was relocated it might not survive conflict resolution in the deployment graph
            if (!node.getRelocations().isEmpty()) {
                ((DefaultDependencyNode) node).setRelocations(List.of());
            }
            if (resolvedDep == null) {
                WorkspaceModule module = null;
                if (resolver.getProjectModuleResolver() != null) {
                    module = resolver.getProjectModuleResolver().getProjectModule(artifact.getGroupId(),
                            artifact.getArtifactId(), artifact.getVersion());
                }
                try {
                    resolvedDep = DependencyUtils.toAppArtifact(getResolvedArtifact(), module)
                            .setOptional(node.getDependency().isOptional())
                            .setScope(node.getDependency().getScope())
                            .setFlags(DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP);
                    if (JavaScopes.PROVIDED.equals(resolvedDep.getScope())) {
                        resolvedDep.setFlags(DependencyFlags.COMPILE_ONLY);
                    }
                    var ext = getExtensionDependencyOrNull();
                    if (ext != null) {
                        resolvedDep.setRuntimeExtensionArtifact();
                        collectConditionalDependencies();
                    }
                } catch (DeploymentInjectionException e) {
                    throw e;
                } catch (Exception t) {
                    throw new DeploymentInjectionException("Failed to inject extension deployment dependencies", t);
                }
            }
        }

        void scheduleChildVisits(ModelResolutionTaskRunner taskRunner,
                BiConsumer<AppDep, ModelResolutionTaskRunner> childVisitor) {
            filterChildren();
            for (var child : children) {
                childVisitor.accept(child, taskRunner);
            }
        }

        /**
         * Filters out dependency nodes that point out to nodes that survived version conflict resolution.
         */
        private void filterChildren() {
            var childNodes = node.getChildren();
            List<DependencyNode> filtered = null;
            for (int i = 0; i < childNodes.size(); ++i) {
                var childNode = childNodes.get(i);
                var winner = getWinner(childNode);
                if (winner == null) {
                    allDeps.add(getCoords(childNode.getArtifact()));
                    children.add(new AppDep(this, childNode));
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
        }

        void setChildFlags(byte walkingFlags) {
            for (var c : children) {
                c.setFlags(walkingFlags);
            }
        }

        void setFlags(byte walkingFlags) {

            resolvedDep.addDependencies(allDeps);

            var existingDep = appBuilder.getDependency(resolvedDep.getKey());
            if (existingDep == null) {
                appBuilder.addDependency(resolvedDep);
                if (ext != null) {
                    managedDeps.add(new Dependency(ext.info.deploymentArtifact, JavaScopes.COMPILE));
                }
            } else if (existingDep != resolvedDep) {
                throw new IllegalStateException(node.getArtifact() + " is already present in the application model");
            }

            resolvedDep.setDirect(isFlagOn(walkingFlags, COLLECT_DIRECT_DEPS));
            if (ext != null) {
                ext.info.ensureActivated(appBuilder);
                if (isFlagOn(walkingFlags, COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
                    walkingFlags = clearFlag(walkingFlags, COLLECT_TOP_EXTENSION_RUNTIME_NODES);
                    resolvedDep.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                }
                if (isFlagOn(walkingFlags, COLLECT_DEPLOYMENT_INJECTION_POINTS)) {
                    walkingFlags = clearFlag(walkingFlags, COLLECT_DEPLOYMENT_INJECTION_POINTS);
                    ext.extDeps = new ArrayList<>();
                    deploymentInjectionPoints.add(this);
                } else if (!ext.presentInTargetGraph) {
                    var parentExtDep = parent;
                    while (parentExtDep != null) {
                        if (parentExtDep.ext != null && parentExtDep.ext.extDeps != null) {
                            parentExtDep.ext.addExtensionDependency(ext);
                            break;
                        }
                        parentExtDep = parentExtDep.parent;
                    }
                }
                ext.info.ensureActivated(appBuilder);
            }
            if (isFlagOn(walkingFlags, COLLECT_RELOADABLE_MODULES)) {
                if (resolvedDep.getWorkspaceModule() != null
                        && !resolvedDep.isFlagSet(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {
                    resolvedDep.setReloadable();
                } else {
                    walkingFlags = clearFlag(walkingFlags, COLLECT_RELOADABLE_MODULES);
                }
            }

            walkingFlags = clearFlag(walkingFlags, COLLECT_DIRECT_DEPS);
            setChildFlags(walkingFlags);
        }

        private ExtensionDependency getExtensionDependencyOrNull() {
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

        /**
         * Collects information about the conditional dependencies and adds them to the processing queue.
         */
        private void collectConditionalDependencies() {
            if (ext == null || ext.info.conditionalDeps.length == 0 || ext.conditionalDepsQueued) {
                return;
            }
            ext.conditionalDepsQueued = true;

            final DependencySelector selector = ext.exclusions == null ? null
                    : new ExclusionDependencySelector(ext.exclusions);
            for (Artifact conditionalArtifact : ext.info.conditionalDeps) {
                if (selector != null && !selector.selectDependency(new Dependency(conditionalArtifact, JavaScopes.RUNTIME))) {
                    continue;
                }
                conditionalArtifact = resolve(conditionalArtifact, ext.runtimeNode.getRepositories());
                final ExtensionInfo condExtInfo = getExtensionInfoOrNull(conditionalArtifact,
                        ext.runtimeNode.getRepositories());
                if (condExtInfo != null && condExtInfo.activated) {
                    continue;
                }
                final ConditionalDependency conditionalDep = new ConditionalDependency(conditionalArtifact, condExtInfo, this);
                conditionalDepsToProcess.add(conditionalDep);
                if (condExtInfo != null) {
                    conditionalDep.conditionalDep.collectConditionalDependencies();
                }
            }
        }

        private void scheduleCollectDeploymentDeps(ModelResolutionTaskRunner taskRunner,
                ConcurrentLinkedDeque<AppDep> injectQueue) {
            var resolvedDep = appBuilder.getDependency(getKey(ext.info.deploymentArtifact));
            if (resolvedDep == null) {
                taskRunner.run(this::collectDeploymentDeps);
                injectQueue.add(this);
            } else {
                // if resolvedDep isn't null, it means the deployment artifact is on the runtime classpath
                // in which case we also clear the reloadable flag on it, in case it's coming from the workspace
                resolvedDep.clearFlag(DependencyFlags.RELOADABLE);
            }
        }

        private void collectDeploymentDeps() {
            ext.collectDeploymentDeps();
        }

        private void injectDeploymentDependency() {
            // if the parent is an extension then add the deployment node as a dependency of the parent's deployment node
            // (that would happen when injecting conditional dependencies)
            // otherwise, the runtime module is going to be replaced with the deployment node
            ext.injectDependencyDependency(parent == null ? null : (parent.ext == null ? null : parent.ext.deploymentNode));
        }
    }

    private static byte clearFlag(byte flags, byte flag) {
        return (flags & flag) > 0 ? (byte) (flags ^ flag) : flags;
    }

    private static boolean isFlagOn(byte flags, byte flag) {
        return (flags & flag) > 0;
    }

    private ExtensionInfo getExtensionInfoOrNull(Artifact artifact, List<RemoteRepository> repos) {
        if (!artifact.getExtension().equals(ArtifactCoords.TYPE_JAR)) {
            return null;
        }
        ExtensionInfo ext = allExtensions.computeIfAbsent(getKey(artifact), k -> resolveExtensionInfo(artifact, repos));
        return ext == EXT_INFO_NONE ? null : ext;
    }

    private ExtensionInfo resolveExtensionInfo(Artifact artifact, List<RemoteRepository> repos) {
        artifact = resolve(artifact, repos);
        final Properties descriptor = PathTree.ofDirectoryOrArchive(artifact.getFile().toPath())
                .apply(BootstrapConstants.DESCRIPTOR_PATH, ApplicationDependencyResolver::readExtensionProperties);
        if (descriptor == null) {
            return EXT_INFO_NONE;
        }
        try {
            return new ExtensionInfo(artifact, descriptor, devMode);
        } catch (BootstrapDependencyProcessingException e) {
            throw new RuntimeException("Failed to collect extension information for " + artifact, e);
        }
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

    private DependencyNode collectDependencies(Artifact artifact, Collection<Exclusion> exclusions,
            List<RemoteRepository> repos) {
        final CollectRequest collectRequest = getCollectRequest(artifact, exclusions, repos);
        DependencyNode root = null;
        try {
            root = resolver.getSystem()
                    .collectDependencies(resolver.getSession(), collectRequest)
                    .getRoot();
        } catch (DependencyCollectionException e) {
            // It could happen, especially in Maven 3.8, that multiple threads could end up writing/reading
            // the same temporary files while resolving the same artifact. Once one of the threads completes
            // resolving the artifact, the temporary file will be renamed to the target artifact file
            // and the other thread will fail with one of the file-not-found exceptions.
            // In this case, we simply re-try the collect request, which should now pick up the already resolved artifact.
            String missingFile = getMissingFileOrNull(e);
            if (missingFile != null) {
                Set<String> missingFiles = new HashSet<>();
                while (missingFile != null) {
                    if (missingFiles.add(missingFile)) {
                        log.debugf("Re-trying the collect request for %s due to missing %s", artifact, missingFile);
                        try {
                            root = resolver.getSystem()
                                    .collectDependencies(resolver.getSession(), collectRequest)
                                    .getRoot();
                            break;
                        } catch (DependencyCollectionException dce) {
                            missingFile = getMissingFileOrNull(dce);
                        }
                    } else {
                        // if it's the second time it's missing, we give up
                        throw wrapInDeploymentInjectionException(artifact, e);
                    }
                }
            }
            if (root == null) {
                throw wrapInDeploymentInjectionException(artifact, e);
            }
        }
        if (root.getChildren().size() != 1) {
            throw new DeploymentInjectionException("Only one child expected but got " + root.getChildren());
        }
        return root.getChildren().get(0);
    }

    private static DeploymentInjectionException wrapInDeploymentInjectionException(Artifact artifact, Exception e) {
        return new DeploymentInjectionException("Failed to collect dependencies for " + artifact, e);
    }

    /**
     * Checks whether the cause of this exception a kind of no-such-file exception and returns the file that was missing.
     *
     * @param dce top level exception
     * @return missing file that was the cause or null, if the cause was different
     */
    private static String getMissingFileOrNull(DependencyCollectionException dce) {
        Throwable t = dce.getCause();
        while (t != null) {
            var cause = t.getCause();
            // It looks like in Maven 3.9 it's NoSuchFileException, while in Maven 3.8 it's FileNotFoundException
            if (cause instanceof NoSuchFileException e) {
                return e.getFile();
            } else if (cause instanceof FileNotFoundException) {
                return cause.getMessage();
            }
            t = cause;
        }
        return null;
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
        private boolean presentInTargetGraph;

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

        void addExtensionDependency(ExtensionDependency dep) {
            if (extDeps == null) {
                extDeps = new ArrayList<>();
            }
            extDeps.add(dep);
        }

        private void collectDeploymentDeps() {
            log.debugf("Collecting dependencies of %s", info.deploymentArtifact);
            deploymentNode = collectDependencies(info.deploymentArtifact, exclusions, runtimeNode.getRepositories());
            if (deploymentNode.getChildren().isEmpty()) {
                throw new RuntimeException(
                        "Failed to collect dependencies of " + deploymentNode.getArtifact()
                                + ": either its POM could not be resolved from the available Maven repositories "
                                + "or the artifact does not have any dependencies while at least a dependency on the runtime artifact "
                                + info.runtimeArtifact + " is expected");
            }
            ensureScopeAndOptionality(deploymentNode, runtimeNode.getDependency().getScope(),
                    runtimeNode.getDependency().isOptional());

            replaceRuntimeExtensionNodes(deploymentNode);
            if (!presentInTargetGraph) {
                throw new RuntimeException(
                        "Quarkus extension deployment artifact " + deploymentNode.getArtifact()
                                + " does not appear to depend on the corresponding runtime artifact "
                                + info.runtimeArtifact);
            }
        }

        private void injectDependencyDependency(DependencyNode parentDeploymentNode) {
            if (parentDeploymentNode == null) {
                runtimeNode.setData(QUARKUS_RUNTIME_ARTIFACT, runtimeNode.getArtifact());
                runtimeNode.setArtifact(deploymentNode.getArtifact());
                runtimeNode.setChildren(deploymentNode.getChildren());
            } else {
                parentDeploymentNode.getChildren().add(deploymentNode);
            }
        }

        void replaceRuntimeExtensionNodes(DependencyNode deploymentNode) {
            var deploymentVisitor = new OrderedDependencyVisitor(deploymentNode);
            // skip the root node
            deploymentVisitor.next();
            int nodesToReplace = extDeps == null ? 1 : extDeps.size() + 1;
            while (deploymentVisitor.hasNext() && nodesToReplace > 0) {
                var node = deploymentVisitor.next();
                if (hasWinner(node)) {
                    continue;
                }
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

        final AppDep conditionalDep;
        private boolean activated;

        private ConditionalDependency(Artifact artifact, ExtensionInfo info, AppDep parent) {
            final DefaultDependencyNode rtNode = new DefaultDependencyNode(
                    new Dependency(artifact, JavaScopes.COMPILE));
            rtNode.setVersion(new BootstrapArtifactVersion(artifact.getVersion()));
            rtNode.setVersionConstraint(new BootstrapArtifactVersionConstraint(
                    new BootstrapArtifactVersion(artifact.getVersion())));
            rtNode.setRepositories(parent.ext.runtimeNode.getRepositories());
            conditionalDep = new AppDep(parent, rtNode);
            conditionalDep.ext = info == null ? null : new ExtensionDependency(info, rtNode, parent.ext.exclusions);
        }

        void activate() {
            if (activated) {
                return;
            }
            activated = true;
            final AppDep parent = conditionalDep.parent;
            final DependencyNode originalNode = collectDependencies(conditionalDep.node.getArtifact(),
                    parent.ext.exclusions,
                    parent.node.getRepositories());
            ensureScopeAndOptionality(originalNode, parent.ext.runtimeNode.getDependency().getScope(),
                    parent.ext.runtimeNode.getDependency().isOptional());

            final DefaultDependencyNode rtNode = (DefaultDependencyNode) conditionalDep.node;
            rtNode.setRepositories(originalNode.getRepositories());
            // if this node has conditional dependencies on its own, they may have been activated by this time
            // in which case they would be included into its children
            List<DependencyNode> currentChildren = rtNode.getChildren();
            if (currentChildren == null || currentChildren.isEmpty()) {
                rtNode.setChildren(originalNode.getChildren());
            } else {
                currentChildren.addAll(originalNode.getChildren());
            }
            if (conditionalDep.ext != null && conditionalDep.ext.extDeps == null) {
                conditionalDep.ext.extDeps = new ArrayList<>();
            }
            visitRuntimeDeps();
            conditionalDep.setFlags(
                    (byte) (COLLECT_DEPLOYMENT_INJECTION_POINTS | (collectReloadableModules ? COLLECT_RELOADABLE_MODULES : 0)));
            if (parent.resolvedDep != null) {
                parent.resolvedDep.addDependency(conditionalDep.resolvedDep.getArtifactCoords());
            }
            parent.ext.runtimeNode.getChildren().add(rtNode);
        }

        private void visitRuntimeDeps() {
            var taskRunner = getTaskRunner();
            conditionalDep.scheduleRuntimeVisit(taskRunner);
            taskRunner.waitForCompletion();
        }

        boolean isSatisfied() {
            var extInfo = conditionalDep.ext == null ? null : conditionalDep.ext.info;
            if (extInfo == null || extInfo.dependencyCondition == null) {
                return true;
            }
            for (ArtifactKey key : extInfo.dependencyCondition) {
                if (!isRuntimeArtifact(key)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Makes sure the node's dependency scope and optionality (including its children) match the expected values.
     *
     * @param node dependency node
     * @param scope expected scope
     * @param optional expected optionality
     */
    private static void ensureScopeAndOptionality(DependencyNode node, String scope, boolean optional) {
        var dep = node.getDependency();
        if (optional == dep.isOptional() && scope.equals(dep.getScope())) {
            return;
        }
        var visitor = new OrderedDependencyVisitor(node);
        while (visitor.hasNext()) {
            dep = visitor.next().getDependency();
            if (optional != dep.isOptional()) {
                visitor.getCurrent().setOptional(optional);
            }
            if (!scope.equals(dep.getScope())) {
                visitor.getCurrent().setScope(scope);
            }
        }
    }

    private static boolean isSameKey(Artifact a1, Artifact a2) {
        return a2.getArtifactId().equals(a1.getArtifactId())
                && a2.getGroupId().equals(a1.getGroupId())
                && a2.getClassifier().equals(a1.getClassifier())
                && a2.getExtension().equals(a1.getExtension());
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
