package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.DependencyNodeUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.jboss.logging.Logger;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentInjectingDependencyVisitor {

    private static final Logger log = Logger.getLogger(DeploymentInjectingDependencyVisitor.class);

    private static final String QUARKUS_RUNTIME_ARTIFACT = "quarkus.runtime";
    private static final String QUARKUS_EXTENSION_DEPENDENCY = "quarkus.ext";

    private static final Artifact[] NO_ARTIFACTS = new Artifact[0];

    public static Artifact getRuntimeArtifact(DependencyNode dep) {
        return (Artifact) dep.getData().get(DeploymentInjectingDependencyVisitor.QUARKUS_RUNTIME_ARTIFACT);
    }

    private final MavenArtifactResolver resolver;
    private final List<Dependency> managedDeps;
    private final List<RemoteRepository> mainRepos;
    private final AppModel.Builder appBuilder;

    private boolean collectingTopRuntimeNodes = true;
    private final List<ExtensionDependency> topExtensionDeps = new ArrayList<>();
    private ExtensionDependency lastVisitedRuntimeExtNode;
    private final Map<AppArtifactKey, ExtensionInfo> allExtensions = new HashMap<>();
    private List<ConditionalDependency> conditionalDepsToProcess = new ArrayList<>();
    private final Deque<Collection<Exclusion>> exclusionStack = new ArrayDeque<>();

    public final Set<AppArtifactKey> allRuntimeDeps = new HashSet<>();

    private final Map<String, CapabilityContract> capabilitiesContracts = new HashMap<>();

    public DeploymentInjectingDependencyVisitor(MavenArtifactResolver resolver, List<Dependency> managedDeps,
            List<RemoteRepository> mainRepos, AppModel.Builder appBuilder) throws BootstrapDependencyProcessingException {
        // we need to be able to take into account whether the deployment dependencies are on an optional dependency branch
        // for that we are going to use a custom dependency selector and re-initialize the resolver to use it
        final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(resolver.getSession());
        final DeploymentDependencySelector depSelector = new DeploymentDependencySelector(session.getDependencySelector());
        session.setDependencySelector(depSelector);
        try {
            this.resolver = new MavenArtifactResolver(new BootstrapMavenContext(BootstrapMavenContext.config()
                    .setRepositorySystem(resolver.getSystem())
                    .setRepositorySystemSession(session)
                    .setRemoteRepositories(resolver.getRepositories())
                    .setRemoteRepositoryManager(resolver.getRemoteRepositoryManager())
                    .setWorkspaceDiscovery(false)));
        } catch (BootstrapMavenException e) {
            throw new BootstrapDependencyProcessingException("Failed to initialize deployment dependencies resolver",
                    e);
        }

        this.managedDeps = managedDeps.isEmpty() ? new ArrayList<>() : managedDeps;
        this.mainRepos = mainRepos;
        this.appBuilder = appBuilder;
    }

    public boolean isInjectedDeps() {
        return !topExtensionDeps.isEmpty();
    }

    public void injectDeploymentDependencies(DependencyNode root) throws BootstrapDependencyProcessingException {
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

        appBuilder.setCapabilitiesContracts(capabilitiesContracts);
    }

    private boolean isRuntimeArtifact(AppArtifactKey key) {
        return allRuntimeDeps.contains(key);
    }

    private void visitRuntimeDependencies(List<DependencyNode> list) {
        int i = 0;
        while (i < list.size()) {
            visitRuntimeDependency(list.get(i++));
        }
    }

    private void visitRuntimeDependency(DependencyNode node) {
        Artifact artifact = node.getArtifact();

        if (allRuntimeDeps.add(DependencyNodeUtils.toKey(artifact))) {
            artifact = resolve(artifact);
            final AppArtifact appArtifact = toAppArtifact(artifact);
            final AppDependency appDep = new AppDependency(appArtifact, node.getDependency().getScope(),
                    node.getDependency().isOptional());
            appBuilder.addRuntimeDep(appDep);
            appBuilder.addFullDeploymentDep(appDep);
        }

        final boolean prevCollectingTopRtNodes = collectingTopRuntimeNodes;
        final ExtensionDependency prevLastVisitedRtExtNode = lastVisitedRuntimeExtNode;

        final boolean popExclusions;
        if (popExclusions = !node.getDependency().getExclusions().isEmpty()) {
            exclusionStack.addLast(node.getDependency().getExclusions());
        }

        try {
            final ExtensionDependency extDep = getExtensionDependencyOrNull(node, artifact);
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
        collectingTopRuntimeNodes = prevCollectingTopRtNodes;
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

        if (collectingTopRuntimeNodes) {
            collectingTopRuntimeNodes = false;
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
        if (!artifact.getExtension().equals("jar")) {
            return null;
        }
        final AppArtifactKey extKey = DependencyNodeUtils.toKey(artifact);
        ExtensionInfo ext = allExtensions.get(extKey);
        if (ext != null) {
            return ext;
        }

        artifact = resolve(artifact);
        final Path path = artifact.getFile().toPath();
        if (Files.isDirectory(path)) {
            ext = createExtensionInfoOrNull(artifact, path.resolve(BootstrapConstants.DESCRIPTOR_PATH));
        } else {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                ext = createExtensionInfoOrNull(artifact, artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH));
            } catch (IOException e) {
                throw new DeploymentInjectionException("Failed to read " + path, e);
            }
        }
        allExtensions.put(extKey, ext);
        return ext;
    }

    private ExtensionInfo createExtensionInfoOrNull(Artifact artifact, Path descriptor)
            throws BootstrapDependencyProcessingException {
        final Properties rtProps = readDescriptor(descriptor);
        if (rtProps == null) {
            return null;
        }
        return new ExtensionInfo(artifact, rtProps);
    }

    private void injectDeploymentDependencies(ExtensionDependency extDep)
            throws BootstrapDependencyProcessingException {
        log.debugf("Injecting deployment dependency %s", extDep.info.deploymentArtifact);
        final DependencyNode deploymentNode = collectDependencies(extDep.info.deploymentArtifact, extDep.exclusions);
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
                    ? resolver.collectDependencies(artifact, Collections.emptyList(), mainRepos, exclusions).getRoot()
                    : resolver
                            .collectManagedDependencies(artifact, Collections.emptyList(), managedDeps, mainRepos, exclusions,
                                    "test",
                                    "provided")
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

    private static Properties readDescriptor(Path path) throws BootstrapDependencyProcessingException {
        if (!Files.exists(path)) {
            // not a platform artifact
            return null;
        }
        final Properties rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            rtProps.load(reader);
        } catch (IOException e) {
            throw new BootstrapDependencyProcessingException("Failed to load " + path, e);
        }
        return rtProps;
    }

    private class ExtensionInfo {
        final Artifact runtimeArtifact;
        final Properties props;
        final Artifact deploymentArtifact;
        final Artifact[] conditionalDeps;
        final AppArtifactKey[] dependencyCondition;
        boolean activated;

        ExtensionInfo(Artifact runtimeArtifact, Properties props) throws BootstrapDependencyProcessingException {
            this.runtimeArtifact = runtimeArtifact;
            this.props = props;

            String value = props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            if (value == null) {
                throw new BootstrapDependencyProcessingException("Extension descriptor from " + runtimeArtifact
                        + " does not include " + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
            }
            Artifact deploymentArtifact = DependencyNodeUtils.toArtifact(value);
            if (deploymentArtifact.getVersion() == null || deploymentArtifact.getVersion().isEmpty()) {
                deploymentArtifact = deploymentArtifact.setVersion(runtimeArtifact.getVersion());
            }
            this.deploymentArtifact = deploymentArtifact;

            value = props.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES);
            if (value != null) {
                final String[] deps = value.split("\\s+");
                conditionalDeps = new Artifact[deps.length];
                for (int i = 0; i < deps.length; ++i) {
                    try {
                        conditionalDeps[i] = DependencyNodeUtils.toArtifact(deps[i]);
                    } catch (Exception e) {
                        throw new BootstrapDependencyProcessingException(
                                "Failed to parse conditional dependencies configuration of " + runtimeArtifact, e);
                    }
                }
            } else {
                conditionalDeps = NO_ARTIFACTS;
            }

            value = props.getProperty(BootstrapConstants.DEPENDENCY_CONDITION);
            if (value != null) {
                final String[] split = value.split("\\s+");
                dependencyCondition = new AppArtifactKey[split.length];
                for (int i = 0; i < split.length; ++i) {
                    final String trigger = split[i];
                    dependencyCondition[i] = AppArtifactKey.fromString(trigger);
                }
            } else {
                dependencyCondition = null;
            }
        }

        void ensureActivated() {
            if (activated) {
                return;
            }
            activated = true;
            appBuilder.handleExtensionProperties(props, runtimeArtifact.toString());

            final String providesCapabilities = props.getProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES);
            if (providesCapabilities != null) {
                capabilitiesContracts.put(toGactv(runtimeArtifact),
                        CapabilityContract.providesCapabilities(toGactv(runtimeArtifact), providesCapabilities));
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
            collectingTopRuntimeNodes = false;
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
            for (AppArtifactKey key : info.dependencyCondition) {
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

    private static AppArtifact toAppArtifact(Artifact artifact) {
        final AppArtifact appArtifact = new AppArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
        final File file = artifact.getFile();
        if (file != null) {
            appArtifact.setPaths(PathsCollection.of(file.toPath()));
        }
        return appArtifact;
    }

    private static String toGactv(Artifact a) {
        return a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getClassifier() + ":" + a.getExtension() + ":"
                + a.getVersion();
    }
}
