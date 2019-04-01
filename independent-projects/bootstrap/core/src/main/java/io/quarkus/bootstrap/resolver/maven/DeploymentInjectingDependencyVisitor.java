/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap.resolver.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.jboss.logging.Logger;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentInjectingDependencyVisitor implements DependencyVisitor {

    private static final Logger log = Logger.getLogger(DeploymentInjectingDependencyVisitor.class);

    static final String INJECTED_DEPENDENCY = "injected.dep";

    private static final DependencyGraphParser graphParser = new DependencyGraphParser();

    public static Artifact getInjectedDependency(DependencyNode dep) {
        return (Artifact) dep.getData().get(DeploymentInjectingDependencyVisitor.INJECTED_DEPENDENCY);
    }

    private final MavenArtifactResolver resolver;
    private DependencyNode node;

    boolean injectedDeps;

    public DeploymentInjectingDependencyVisitor(MavenArtifactResolver resolver) {
        this.resolver = resolver;
    }

    public boolean isInjectedDeps() {
        return injectedDeps;
    }

    @Override
    public boolean visitEnter(DependencyNode node) {

        final Artifact artifact = node.getArtifact();
        if(!artifact.getExtension().equals("jar")) {
            return true;
        }
        this.node = node;

        boolean processChildren = true;
        final Path path = resolve(artifact);
        try {
            if (Files.isDirectory(path)) {
                processChildren = !processMetaInfDir(path.resolve(BootstrapConstants.META_INF));
            } else {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                    processChildren = !processMetaInfDir(artifactFs.getPath(BootstrapConstants.META_INF));
                }
            }
        } catch (Throwable t) {
            throw new DeploymentInjectionException("Failed to inject extension deplpyment dependencies", t);
        }
        return processChildren;
    }

    private boolean processMetaInfDir(Path metaInfDir) throws BootstrapDependencyProcessingException {
        if (Files.exists(metaInfDir)) {
            Path p = metaInfDir.resolve(BootstrapConstants.DEPLOYMENT_DEPENDENCY_GRAPH);
            if (Files.exists(p)) {
                processDeploymentDependencyGraph(p);
                return true;
            }
            p = metaInfDir.resolve(BootstrapConstants.DESCRIPTOR_PATH);
            if (Files.exists(p)) {
                processPlatformArtifact(p);
                return true;
            }
        }
        return false;
    }

    private void processPlatformArtifact(Path descriptor) throws BootstrapDependencyProcessingException {
        final Properties rtProps = resolveDescriptor(descriptor);
        if(rtProps == null) {
            return;
        }
        log.debugf("Processing platform dependency %s", node);

        String value = rtProps.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        if(value != null) {
            replaceWith(collectDependencies(DependencyGraphParser.toArtifact(value)));
        }
    }

    private void processDeploymentDependencyGraph(Path graphPath) throws BootstrapDependencyProcessingException {
        final DependencyNode parsedGraph = graphParser.parse(graphPath);
        //BootstrapDependencyGraphTransformer.log("Parsed graph", parsedGraph);
        //BootstrapDependencyGraphTransformer.log("Resolved graph", collectDependencies(parsedGraph.getArtifact()));
        replaceWith(parsedGraph);
    }

    private void replaceWith(DependencyNode depNode) throws BootstrapDependencyProcessingException {
        //BootstrapDependencyGraphTransformer.log("Replacing dependency " + depNode.getArtifact(), depNode);
        node.setData(INJECTED_DEPENDENCY, node.getArtifact());
        node.setArtifact(depNode.getArtifact());
        node.getDependency().setArtifact(depNode.getArtifact());
        node.setChildren(depNode.getChildren());
        injectedDeps = true;
    }

    private DependencyNode collectDependencies(Artifact artifact) throws BootstrapDependencyProcessingException {
        if(artifact.getVersion().isEmpty()) {
            artifact = artifact.setVersion(node.getArtifact().getVersion());
        }
        try {
            return resolver.collectDependencies(artifact).getRoot();
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

    @Override
    public boolean visitLeave(DependencyNode node) {
        return true;
    }
}
