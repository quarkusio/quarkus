package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;
import io.quarkus.paths.PathTree;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.jboss.logging.Logger;

public class ApplicationDependencyTreeResolver {

    private static final Logger log = Logger.getLogger(ApplicationDependencyTreeResolver.class);

    private static final String QUARKUS_RUNTIME_ARTIFACT = "quarkus.runtime";
    private static final String QUARKUS_EXTENSION_DEPENDENCY = "quarkus.ext";

    /* @formatter:off */
    private static final byte COLLECT_TOP_EXTENSION_RUNTIME_NODES = 0b001;
    private static final byte COLLECT_DIRECT_DEPS =                 0b010;
    private static final byte COLLECT_RELOADABLE_MODULES =          0b100;
    /* @formatter:on */

    // this is a temporary option, to enable the previous way of initializing runtime classpath dependencies
    private static final boolean CONVERGED_TREE_ONLY = PropertyUtils.getBoolean("quarkus.bootstrap.converged-tree-only", false);

    private static final Artifact[] NO_ARTIFACTS = new Artifact[0];

    public static ApplicationDependencyTreeResolver newInstance() {
        return new ApplicationDependencyTreeResolver();
    }

    public static Artifact getRuntimeArtifact(DependencyNode dep) {
        return (Artifact) dep.getData().get(QUARKUS_RUNTIME_ARTIFACT);
    }

    private byte walkingFlags = COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS;
    private final List<ExtensionDependency> topExtensionDeps = new ArrayList<>();
    private ExtensionDependency lastVisitedRuntimeExtNode;
    private final Map<ArtifactKey, ExtensionInfo> allExtensions = new HashMap<>();
    private List<ConditionalDependency> conditionalDepsToProcess = new ArrayList<>();
    private final Deque<Collection<Exclusion>> exclusionStack = new ArrayDeque<>();

    private final Map<ArtifactCoords, Set<ArtifactKey>> artifactDeps = new HashMap<>();
    private final Map<ArtifactKey, ResolvedDependencyBuilder> appDeps = new LinkedHashMap<>();

    private MavenArtifactResolver resolver;
    private List<Dependency> managedDeps;
    private List<RemoteRepository> mainRepos;
    private ApplicationModelBuilder appBuilder;
    private boolean collectReloadableModules;
    private Consumer<String> buildTreeConsumer;

    public ApplicationDependencyTreeResolver setArtifactResolver(MavenArtifactResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    public ApplicationDependencyTreeResolver setManagedDependencies(List<Dependency> managedDeps) {
        this.managedDeps = managedDeps;
        return this;
    }

    public ApplicationDependencyTreeResolver setMainRepositories(List<RemoteRepository> mainRepos) {
        this.mainRepos = mainRepos;
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

    public void resolve(CollectRequest collectRtDepsRequest) throws AppModelResolverException {

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

        List<ConditionalDependency> activatedConditionalDeps = Collections.emptyList();

        if (!conditionalDepsToProcess.isEmpty()) {
            activatedConditionalDeps = new ArrayList<>();
            List<ConditionalDependency> unsatisfiedConditionalDeps = new ArrayList<>();
            while (!conditionalDepsToProcess.isEmpty()) {
                final List<ConditionalDependency> tmp = unsatisfiedConditionalDeps;
                unsatisfiedConditionalDeps = conditionalDepsToProcess;
                conditionalDepsToProcess = tmp;
                final int totalConditionsToProcess = unsatisfiedConditionalDeps.size();
                final Iterator<ConditionalDependency> i = unsatisfiedConditionalDeps.iterator();
                while (i.hasNext()) {
                    final ConditionalDependency cd = i.next();
                    final boolean satisfied = cd.isSatisfied();
                    if (!satisfied) {
                        continue;
                    }
                    i.remove();

                    cd.activate();
                    activatedConditionalDeps.add(cd);
                }
                if (totalConditionsToProcess == unsatisfiedConditionalDeps.size()) {
                    // none of the dependencies was satisfied
                    break;
                }
                conditionalDepsToProcess.addAll(unsatisfiedConditionalDeps);
                unsatisfiedConditionalDeps.clear();
            }
        }

        // resolve and inject deployment dependency branches for the top (first met) runtime extension nodes
        for (ExtensionDependency extDep : topExtensionDeps) {
            injectDeploymentDependencies(extDep);
        }

        if (!activatedConditionalDeps.isEmpty()) {
            for (ConditionalDependency cd : activatedConditionalDeps) {
                injectDeploymentDependencies(cd.getExtensionDependency());
            }
        }

        root = normalize(originalSession, root);

        final BuildDependencyGraphVisitor buildDepsVisitor = new BuildDependencyGraphVisitor(originalResolver, appDeps,
                buildTreeConsumer);
        buildDepsVisitor.visit(root);

        if (!CONVERGED_TREE_ONLY && collectReloadableModules) {
            final Set<ArtifactKey> visited = new HashSet<>();
            for (ResolvedDependencyBuilder db : appDeps.values()) {
                if (!db.isFlagSet(DependencyFlags.RELOADABLE)) {
                    clearReloadableFlag(db, visited);
                }
            }
        }

        for (ResolvedDependencyBuilder db : appDeps.values()) {
            appBuilder.addDependency(db.build());
        }

        collectPlatformProperties();
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

    private void clearReloadableFlag(ResolvedDependencyBuilder db, Set<ArtifactKey> visited) {
        final GACTV coords = new GACTV(db.getGroupId(), db.getArtifactId(), db.getClassifier(), db.getType(), db.getVersion());
        final Set<ArtifactKey> deps = artifactDeps.get(coords);
        if (deps == null || deps.isEmpty()) {
            return;
        }
        for (ArtifactKey key : deps) {
            final ResolvedDependencyBuilder dep = appDeps.get(key);
            if (dep == null || !visited.add(key)) {
                continue;
            }
            dep.clearFlag(DependencyFlags.RELOADABLE);
            clearReloadableFlag(dep, visited);
            visited.remove(key);
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
        RepositorySystemSession session = resolver.getSession();
        if (!CONVERGED_TREE_ONLY && collectReloadableModules) {
            final DefaultRepositorySystemSession mutableSession;
            mutableSession = new DefaultRepositorySystemSession(resolver.getSession());
            mutableSession.setDependencyGraphTransformer(new DependencyGraphTransformer() {

                @Override
                public DependencyNode transformGraph(DependencyNode node, DependencyGraphTransformationContext context)
                        throws RepositoryException {
                    for (DependencyNode c : node.getChildren()) {
                        walk(c);
                    }
                    return resolver.getSession().getDependencyGraphTransformer().transformGraph(node, context);
                }

                private void walk(DependencyNode node) {
                    if (node.getChildren().isEmpty()) {
                        return;
                    }
                    final Set<ArtifactKey> deps = artifactDeps
                            .computeIfAbsent(DependencyUtils.getCoords(node.getArtifact()),
                                    k -> new HashSet<>(node.getChildren().size()));
                    for (DependencyNode c : node.getChildren()) {
                        deps.add(getKey(c.getArtifact()));
                        walk(c);
                    }
                }
            });
            session = mutableSession;
        }
        try {
            return resolver.getSystem().resolveDependencies(session, new DependencyRequest().setCollectRequest(request))
                    .getRoot();
        } catch (DependencyResolutionException e) {
            final Artifact a = request.getRoot() == null ? request.getRootArtifact() : request.getRoot().getArtifact();
            throw new BootstrapMavenException("Failed to resolve dependencies for " + a, e);
        }
    }

    private boolean isRuntimeArtifact(ArtifactKey key) {
        final ResolvedDependencyBuilder dep = appDeps.get(key);
        return dep != null && dep.isFlagSet(DependencyFlags.RUNTIME_CP);
    }

    private void visitRuntimeDependencies(List<DependencyNode> list) {
        for (DependencyNode n : list) {
            visitRuntimeDependency(n);
        }
    }

    private void visitRuntimeDependency(DependencyNode node) {

        final byte prevWalkingFlags = walkingFlags;
        final ExtensionDependency prevLastVisitedRtExtNode = lastVisitedRuntimeExtNode;

        final boolean popExclusions;
        if (popExclusions = !node.getDependency().getExclusions().isEmpty()) {
            exclusionStack.addLast(node.getDependency().getExclusions());
        }

        Artifact artifact = node.getArtifact();
        final ArtifactKey key = getKey(artifact);
        ResolvedDependencyBuilder dep = appDeps.get(key);
        if (dep == null) {
            artifact = resolve(artifact);
        }

        try {
            final ExtensionDependency extDep = getExtensionDependencyOrNull(node, artifact);

            if (dep == null) {
                WorkspaceModule module = null;
                if (resolver.getProjectModuleResolver() != null) {
                    module = resolver.getProjectModuleResolver().getProjectModule(artifact.getGroupId(),
                            artifact.getArtifactId());
                }
                dep = toAppArtifact(artifact, module)
                        .setRuntimeCp()
                        .setDeploymentCp()
                        .setOptional(node.getDependency().isOptional())
                        .setScope(node.getDependency().getScope())
                        .setDirect(isWalkingFlagOn(COLLECT_DIRECT_DEPS));
                if (extDep != null) {
                    dep.setRuntimeExtensionArtifact();
                    if (isWalkingFlagOn(COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
                        dep.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                    }
                }
                if (isWalkingFlagOn(COLLECT_RELOADABLE_MODULES)) {
                    if (module != null) {
                        dep.setReloadable();
                        appBuilder.addReloadableWorkspaceModule(key);
                    } else {
                        clearWalkingFlag(COLLECT_RELOADABLE_MODULES);
                    }
                }
                appDeps.put(key, dep);
            }
            clearWalkingFlag(COLLECT_DIRECT_DEPS);

            if (extDep != null) {
                extDep.info.ensureActivated();
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
        lastVisitedRuntimeExtNode = prevLastVisitedRtExtNode;
    }

    private ExtensionDependency getExtensionDependencyOrNull(DependencyNode node, Artifact artifact)
            throws BootstrapDependencyProcessingException {
        ExtensionDependency extDep = ExtensionDependency.get(node);
        if (extDep != null) {
            return extDep;
        }
        final ExtensionInfo extInfo = getExtensionInfoOrNull(artifact);
        if (extInfo != null) {
            Collection<Exclusion> exclusions;
            if (!exclusionStack.isEmpty()) {
                if (exclusionStack.size() == 1) {
                    exclusions = exclusionStack.peekLast();
                } else {
                    exclusions = new ArrayList<>();
                    for (Collection<Exclusion> set : exclusionStack) {
                        exclusions.addAll(set);
                    }
                }
            } else {
                exclusions = Collections.emptyList();
            }
            return new ExtensionDependency(extInfo, node, exclusions);
        }
        return null;
    }

    private void visitExtensionDependency(ExtensionDependency extDep)
            throws BootstrapDependencyProcessingException {

        managedDeps.add(new Dependency(extDep.info.deploymentArtifact, JavaScopes.COMPILE));

        collectConditionalDependencies(extDep);

        if (isWalkingFlagOn(COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
            clearWalkingFlag(COLLECT_TOP_EXTENSION_RUNTIME_NODES);
            topExtensionDeps.add(extDep);
        }
        if (lastVisitedRuntimeExtNode != null) {
            lastVisitedRuntimeExtNode.addExtensionDependency(extDep);
        }
        lastVisitedRuntimeExtNode = extDep;
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
            if (selector != null && !selector.selectDependency(new Dependency(conditionalArtifact, "runtime"))) {
                continue;
            }
            final ExtensionInfo conditionalInfo = getExtensionInfoOrNull(conditionalArtifact);
            final ConditionalDependency conditionalDep = new ConditionalDependency(conditionalInfo, dependent);
            conditionalDepsToProcess.add(conditionalDep);
            collectConditionalDependencies(conditionalDep.getExtensionDependency());
        }
    }

    private ExtensionInfo getExtensionInfoOrNull(Artifact artifact) throws BootstrapDependencyProcessingException {
        if (!artifact.getExtension().equals(ArtifactCoords.TYPE_JAR)) {
            return null;
        }
        final ArtifactKey extKey = getKey(artifact);
        ExtensionInfo ext = allExtensions.get(extKey);
        if (ext != null) {
            return ext;
        }

        artifact = resolve(artifact);
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
        if (descriptor != null) {
            ext = new ExtensionInfo(artifact, descriptor);
            allExtensions.put(extKey, ext);
        }
        return ext;
    }

    private void injectDeploymentDependencies(ExtensionDependency extDep)
            throws BootstrapDependencyProcessingException {
        log.debugf("Injecting deployment dependency %s", extDep.info.deploymentArtifact);
        final DependencyNode deploymentNode = collectDependencies(extDep.info.deploymentArtifact, extDep.exclusions);
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

        final List<DependencyNode> deploymentDeps = deploymentNode.getChildren();
        if (!replaceDirectDepBranch(extDep, deploymentDeps)) {
            throw new BootstrapDependencyProcessingException(
                    "Quarkus extension deployment artifact " + deploymentNode.getArtifact()
                            + " does not appear to depend on the corresponding runtime artifact "
                            + extDep.info.runtimeArtifact);
        }

        final DependencyNode runtimeNode = extDep.runtimeNode;
        runtimeNode.setData(QUARKUS_RUNTIME_ARTIFACT, runtimeNode.getArtifact());
        runtimeNode.setArtifact(deploymentNode.getArtifact());
        runtimeNode.getDependency().setArtifact(deploymentNode.getArtifact());
        runtimeNode.setChildren(deploymentDeps);
    }

    private void clearReloadable(DependencyNode node) {
        for (DependencyNode child : node.getChildren()) {
            clearReloadable(child);
        }
        final ResolvedDependencyBuilder dep = appDeps.get(getKey(node.getArtifact()));
        if (dep != null) {
            dep.clearFlag(DependencyFlags.RELOADABLE);
        }
    }

    private boolean replaceDirectDepBranch(ExtensionDependency extDep, List<DependencyNode> deploymentDeps)
            throws BootstrapDependencyProcessingException {
        int i = 0;
        DependencyNode inserted = null;
        while (i < deploymentDeps.size()) {
            final Artifact a = deploymentDeps.get(i).getArtifact();
            if (a == null) {
                continue;
            }
            if (isSameKey(extDep.info.runtimeArtifact, a)) {
                // we are not comparing the version in the above condition because the runtime version
                // may appear to be different then the deployment one and that's ok
                // e.g. the version of the runtime artifact could be managed by a BOM
                // but overridden by the user in the project config. The way the deployment deps
                // are resolved here, the deployment version of the runtime artifact will be the one from the BOM.
                inserted = new DefaultDependencyNode(extDep.runtimeNode);
                inserted.setChildren(extDep.runtimeNode.getChildren());
                deploymentDeps.set(i, inserted);
                break;
            }
            ++i;
        }
        if (inserted == null) {
            return false;
        }

        if (extDep.runtimeExtensionDeps != null) {
            for (ExtensionDependency dep : extDep.runtimeExtensionDeps) {
                for (DependencyNode deploymentDep : deploymentDeps) {
                    if (deploymentDep == inserted) {
                        continue;
                    }
                    if (replaceRuntimeBranch(dep, deploymentDep.getChildren())) {
                        break;
                    }
                }
            }
        }

        return true;
    }

    private boolean replaceRuntimeBranch(ExtensionDependency extNode, List<DependencyNode> deploymentNodes)
            throws BootstrapDependencyProcessingException {
        if (replaceDirectDepBranch(extNode, deploymentNodes)) {
            return true;
        }
        for (DependencyNode deploymentNode : deploymentNodes) {
            if (replaceRuntimeBranch(extNode, deploymentNode.getChildren())) {
                return true;
            }
        }
        return false;
    }

    private DependencyNode collectDependencies(Artifact artifact, Collection<Exclusion> exclusions)
            throws BootstrapDependencyProcessingException {
        try {
            return managedDeps.isEmpty()
                    ? resolver.collectDependencies(artifact, List.of(), mainRepos, exclusions).getRoot()
                    : resolver
                            .collectManagedDependencies(artifact, List.of(), managedDeps, mainRepos, exclusions,
                                    JavaScopes.TEST, JavaScopes.PROVIDED)
                            .getRoot();
        } catch (AppModelResolverException e) {
            throw new DeploymentInjectionException(e);
        }
    }

    private Artifact resolve(Artifact artifact) {
        if (artifact.getFile() != null) {
            return artifact;
        }
        try {
            return resolver.resolve(artifact).getArtifact();
        } catch (AppModelResolverException e) {
            throw new DeploymentInjectionException(e);
        }
    }

    private void setWalkingFlag(byte flag) {
        walkingFlags |= flag;
    }

    private boolean isWalkingFlagOn(byte flag) {
        return (walkingFlags & flag) > 0;
    }

    private void clearWalkingFlag(byte flag) {
        if ((walkingFlags & flag) > 0) {
            walkingFlags ^= flag;
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

        ExtensionInfo(Artifact runtimeArtifact, Properties props) throws BootstrapDependencyProcessingException {
            this.runtimeArtifact = runtimeArtifact;
            this.props = props;

            String value = props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            if (value == null) {
                throw new BootstrapDependencyProcessingException("Extension descriptor from " + runtimeArtifact
                        + " does not include " + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            }
            Artifact deploymentArtifact = DependencyUtils.toArtifact(value);
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
                        conditionalDeps[i] = DependencyUtils.toArtifact(deps[i]);
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

    private static class ExtensionDependency {

        static ExtensionDependency get(DependencyNode node) {
            return (ExtensionDependency) node.getData().get(QUARKUS_EXTENSION_DEPENDENCY);
        }

        final ExtensionInfo info;
        final DependencyNode runtimeNode;
        final Collection<Exclusion> exclusions;
        boolean conditionalDepsQueued;
        private List<ExtensionDependency> runtimeExtensionDeps;

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
            if (runtimeExtensionDeps == null) {
                runtimeExtensionDeps = new ArrayList<>();
            }
            runtimeExtensionDeps.add(dep);
        }
    }

    private class ConditionalDependency {

        final ExtensionInfo info;
        final ExtensionDependency dependent;
        private ExtensionDependency dependency;
        private boolean activated;

        private ConditionalDependency(ExtensionInfo info, ExtensionDependency dependent) {
            this.info = Objects.requireNonNull(info, "Extension info is null");
            this.dependent = dependent;
        }

        ExtensionDependency getExtensionDependency() {
            if (dependency == null) {
                final DefaultDependencyNode rtNode = new DefaultDependencyNode(new Dependency(info.runtimeArtifact, "runtime"));
                rtNode.setVersion(new BootstrapArtifactVersion(info.runtimeArtifact.getVersion()));
                rtNode.setVersionConstraint(new BootstrapArtifactVersionConstraint(
                        new BootstrapArtifactVersion(info.runtimeArtifact.getVersion())));
                dependency = new ExtensionDependency(info, rtNode, dependent.exclusions);
            }
            return dependency;
        }

        void activate() throws BootstrapDependencyProcessingException {
            if (activated) {
                return;
            }
            activated = true;
            clearWalkingFlag(COLLECT_TOP_EXTENSION_RUNTIME_NODES);
            final ExtensionDependency extDep = getExtensionDependency();
            final DependencyNode originalNode = collectDependencies(info.runtimeArtifact, extDep.exclusions);
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
            visitRuntimeDependency(rtNode);
            dependent.runtimeNode.getChildren().add(rtNode);
        }

        boolean isSatisfied() throws BootstrapDependencyProcessingException {
            if (info.dependencyCondition == null) {
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

    private static ArtifactKey getKey(Artifact a) {
        return DependencyUtils.getKey(a);
    }

    public static ResolvedDependencyBuilder toAppArtifact(Artifact artifact, WorkspaceModule module) {
        return ResolvedDependencyBuilder.newInstance()
                .setWorkspaceModule(module)
                .setGroupId(artifact.getGroupId())
                .setArtifactId(artifact.getArtifactId())
                .setClassifier(artifact.getClassifier())
                .setType(artifact.getExtension())
                .setVersion(artifact.getVersion())
                .setResolvedPaths(artifact.getFile() == null ? PathList.empty() : PathList.of(artifact.getFile().toPath()));
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
}
