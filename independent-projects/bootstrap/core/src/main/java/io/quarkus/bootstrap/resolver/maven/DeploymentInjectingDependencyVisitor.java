package io.quarkus.bootstrap.resolver.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jboss.logging.Logger;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentInjectingDependencyVisitor {


    private static final Logger log = Logger.getLogger(DeploymentInjectingDependencyVisitor.class);

    static final String QUARKUS_RUNTIME_ARTIFACT = "quarkus.runtime";
    private static final String QUARKUS_DEPLOYMENT_ARTIFACT = "quarkus.deployment";

    public static Artifact getRuntimeArtifact(DependencyNode dep) {
        return (Artifact) dep.getData().get(DeploymentInjectingDependencyVisitor.QUARKUS_RUNTIME_ARTIFACT);
    }

    private final MavenArtifactResolver resolver;
    private final List<Dependency> managedDeps;
    private final List<Dependency> runtimeExtensionDeps = new ArrayList<>();
    private final List<RemoteRepository> mainRepos;

    boolean injectedDeps;

    private List<DependencyNode> runtimeNodes = new ArrayList<>();
    private final AppModel.Builder appBuilder;

    public DeploymentInjectingDependencyVisitor(MavenArtifactResolver resolver, List<Dependency> managedDeps, List<RemoteRepository> mainRepos, AppModel.Builder appBuilder) {
        this.resolver = resolver;
        this.managedDeps = managedDeps.isEmpty() ? new ArrayList<>() : managedDeps;
        this.mainRepos = mainRepos;
        this.appBuilder = appBuilder;
    }

    public boolean isInjectedDeps() {
        return injectedDeps;
    }

    public void injectDeploymentDependencies(DependencyNode root) throws BootstrapDependencyProcessingException {
        collectRuntimeExtensions(root.getChildren());
        // resolve and inject deployment dependencies
        for(DependencyNode rtNode : runtimeNodes) {
            replaceWith(rtNode, collectDependencies((Artifact)rtNode.getData().get(QUARKUS_DEPLOYMENT_ARTIFACT)));
        }
    }

    public void collectRuntimeExtensions(List<DependencyNode> list) {
        if(list.isEmpty()) {
            return;
        }
        int i = 0;
        while (i < list.size()) {
            collectRuntimeExtensions(list.get(i++));
        }
    }

    private void collectRuntimeExtensions(DependencyNode node) {
        final Artifact artifact = node.getArtifact();
        if(!artifact.getExtension().equals("jar")) {
            return;
        }
        final Path path = resolve(artifact);
        try {
            if (Files.isDirectory(path)) {
                processMetaInfDir(node, path.resolve(BootstrapConstants.META_INF));
            } else {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                    processMetaInfDir(node, artifactFs.getPath(BootstrapConstants.META_INF));
                }
            }
        } catch (Exception t) {
            throw new DeploymentInjectionException("Failed to inject extension deplpyment dependencies", t);
        }
        collectRuntimeExtensions(node.getChildren());
    }

    private void processMetaInfDir(DependencyNode node, Path metaInfDir) throws BootstrapDependencyProcessingException {
        if (!Files.exists(metaInfDir)) {
            return;
        }
        final Path p = metaInfDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
        if (!Files.exists(p)) {
            return;
        }
        processPlatformArtifact(node, p);
    }

    private void processPlatformArtifact(DependencyNode node, Path descriptor) throws BootstrapDependencyProcessingException {
        final Properties rtProps = resolveDescriptor(descriptor);
        if(rtProps == null) {
            return;
        }
        final String value = rtProps.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        appBuilder.handleExtensionProperties(rtProps, node.getArtifact().toString());
        if(value == null) {
            return;
        }
        Artifact deploymentArtifact = toArtifact(value);
        if(deploymentArtifact.getVersion() == null || deploymentArtifact.getVersion().isEmpty()) {
            deploymentArtifact = deploymentArtifact.setVersion(node.getArtifact().getVersion());
        }
        node.setData(QUARKUS_DEPLOYMENT_ARTIFACT, deploymentArtifact);
        runtimeNodes.add(node);
        Dependency dependency = new Dependency(node.getArtifact(), JavaScopes.COMPILE);
        managedDeps.add(dependency);
        runtimeExtensionDeps.add(dependency);
        managedDeps.add(new Dependency(deploymentArtifact, JavaScopes.COMPILE));
    }

    public List<Dependency> getRuntimeExtensionDeps() {
        return runtimeExtensionDeps;
    }

    private void replaceWith(DependencyNode originalNode, DependencyNode newNode) throws BootstrapDependencyProcessingException {
        List<DependencyNode> children = newNode.getChildren();
        if (children.isEmpty()) {
            throw new BootstrapDependencyProcessingException(
                    "No dependencies collected for Quarkus extension deployment artifact " + newNode.getArtifact()
                            + " while at least the corresponding runtime artifact " + originalNode.getArtifact() + " is expected");
        }
        log.debugf("Injecting deployment dependency %s", newNode);

        originalNode.setData(QUARKUS_RUNTIME_ARTIFACT, originalNode.getArtifact());
        originalNode.setArtifact(newNode.getArtifact());
        originalNode.getDependency().setArtifact(newNode.getArtifact());
        originalNode.setChildren(children);
        injectedDeps = true;
    }

    private DependencyNode collectDependencies(Artifact artifact) throws BootstrapDependencyProcessingException {
        try {
            return managedDeps.isEmpty() ? resolver.collectDependencies(artifact, Collections.emptyList(), mainRepos).getRoot()
                    : resolver.collectManagedDependencies(artifact, Collections.emptyList(), managedDeps, mainRepos, "test").getRoot();
        } catch (AppModelResolverException e) {
            throw new DeploymentInjectionException(e);
        }
    }

    private Path resolve(Artifact artifact) {
        File file = artifact.getFile();
        if(file != null) {
            return file.toPath();
        }
        try {
            return resolver.resolve(artifact).getArtifact().getFile().toPath();
        } catch (AppModelResolverException e) {
            throw new DeploymentInjectionException(e);
        }
    }

    private Properties resolveDescriptor(final Path path) throws BootstrapDependencyProcessingException {
        final Properties rtProps;
        if (!Files.exists(path)) {
            // not a platform artifact
            return null;
        }
        rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            rtProps.load(reader);
        } catch (IOException e) {
            throw new BootstrapDependencyProcessingException("Failed to load " + path, e);
        }
        return rtProps;
    }

    public static Artifact toArtifact(String str) {
        return toArtifact(str, 0);
    }

    public static Artifact toArtifact(String str, int offset) {
        String groupId = null;
        String artifactId = null;
        String classifier = "";
        String type = "jar";
        String version = null;

        int colon = str.indexOf(':', offset);
        final int length = str.length();
        if(colon < offset + 1 || colon == length - 1) {
            illegalDependencyFormat(str);
        }
        groupId = str.substring(offset, colon);
        offset = colon + 1;
        colon = str.indexOf(':', offset);
        if(colon < 0) {
            artifactId = str.substring(offset, length);
        } else {
            if(colon == length - 1) {
                illegalDependencyFormat(str);
            }
            artifactId = str.substring(offset, colon);
            offset = colon + 1;
            colon = str.indexOf(':', offset);
            if(colon < 0) {
                version = str.substring(offset, length);
            } else {
                if(colon == length - 1) {
                    illegalDependencyFormat(str);
                }
                type = str.substring(offset, colon);
                offset = colon + 1;
                colon = str.indexOf(':', offset);
                if(colon < 0) {
                    version = str.substring(offset, length);
                } else {
                    if (colon == length - 1) {
                        illegalDependencyFormat(str);
                    }
                    classifier = type;
                    type = str.substring(offset, colon);
                    version = str.substring(colon + 1);
                }
            }
        }
        return new DefaultArtifact(groupId, artifactId, classifier, type, version);
    }

    private static void illegalDependencyFormat(String str) {
        throw new IllegalArgumentException("Bad artifact coordinates " + str
                + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
    }
}
