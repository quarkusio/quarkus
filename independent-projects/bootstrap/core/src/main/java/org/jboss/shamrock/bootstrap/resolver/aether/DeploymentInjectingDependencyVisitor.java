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

package org.jboss.shamrock.bootstrap.resolver.aether;

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
import org.jboss.shamrock.bootstrap.BootstrapConstants;
import org.jboss.shamrock.bootstrap.BootstrapDependencyProcessingException;
import org.jboss.shamrock.bootstrap.resolver.AppArtifactResolverException;
import org.jboss.shamrock.bootstrap.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentInjectingDependencyVisitor implements DependencyVisitor {

    private static final Logger log = Logger.getLogger(DeploymentInjectingDependencyVisitor.class);

    static final String INJECTED_DEPENDENCY = "injected.dep";

    private static final DependencyGraphParser graphParser = new DependencyGraphParser();

    private final AetherArtifactResolver resolver;
    private DependencyNode node;

    boolean injectedDeps;

    public DeploymentInjectingDependencyVisitor(AetherArtifactResolver resolver) {
        this.resolver = resolver;
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
                Path p = path.resolve(BootstrapConstants.DEPLOYMENT_DEPENDENCY_GRAPH);
                if (Files.exists(p)) {
                    processDeploymentDependencyGraph(p);
                    processChildren = false;
                } else {
                    p = path.resolve(BootstrapConstants.DESCRIPTOR_PATH);
                    if (Files.exists(p)) {
                        processPlatformArtifact(p);
                        processChildren = false;
                    }
                }
            } else {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                    Path p = artifactFs.getPath(BootstrapConstants.DEPLOYMENT_DEPENDENCY_GRAPH);
                    if (Files.exists(p)) {
                        processDeploymentDependencyGraph(p);
                        processChildren = false;
                    } else {
                        p = artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                        if (Files.exists(p)) {
                            processPlatformArtifact(p);
                            processChildren = false;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new DeploymentInjectionException("Failed to inject extension deplpyment dependencies", t);
        }
        return processChildren;
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
        node.setArtifact(depNode.getArtifact());
        node.setChildren(depNode.getChildren());
        node.setData(INJECTED_DEPENDENCY, INJECTED_DEPENDENCY);
        injectedDeps = true;
    }

    private DependencyNode collectDependencies(Artifact artifact) throws BootstrapDependencyProcessingException {
        if(artifact.getVersion().isEmpty()) {
            artifact = artifact.setVersion(node.getArtifact().getVersion());
        }
        try {
            return resolver.collectDependencies(artifact);
        } catch (AppArtifactResolverException e) {
            throw new DeploymentInjectionException(e);
        }
    }

    private Path resolve(Artifact artifact) {
        File file = artifact.getFile();
        if(file != null) {
            return file.toPath();
        }
        try {
            return resolver.doResolve(artifact).getArtifact().getFile().toPath();
        } catch (AppArtifactResolverException e) {
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
            throw new BootstrapDependencyProcessingException("Failed to load ");
        }
        return rtProps;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        return true;
    }
}
