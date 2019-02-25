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

package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.aether.DependencyGraphParser;

/**
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "extension-descriptor", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExtensionDescriptorMojo extends AbstractMojo {

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> repos;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(required = true)
    private String deployment;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Properties props = new Properties();
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment);

        final Path output = outputDirectory.toPath().resolve(BootstrapConstants.QUARKUS);
        try {
            Files.createDirectories(output);
            try (BufferedWriter writer = Files.newBufferedWriter(output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME))) {
                props.store(writer, "Generated by extension-descriptor");
            }
        } catch(IOException e) {
            throw new MojoExecutionException("Failed to persist extension descriptor " + output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME), e);
        }

        persistDependencyGraph(output);
    }

    private void persistDependencyGraph(Path output) throws MojoExecutionException {

        final Artifact artifact = DependencyGraphParser.toArtifact(deployment);

        final ArtifactDescriptorRequest descrReq = new ArtifactDescriptorRequest();
        descrReq.setArtifact(artifact);
        final ArtifactDescriptorResult artDescr;
        try {
            artDescr = repoSystem.readArtifactDescriptor(repoSession, descrReq);
        } catch (ArtifactDescriptorException e) {
            throw new MojoExecutionException("Failed to read descriptor of " + artifact, e);
        }

        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, "runtime"));
        collectRequest.setRepositories(artDescr.getRepositories());
        final DependencyNode root;
        try {
            root = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
        } catch (DependencyCollectionException e) {
            throw new MojoExecutionException("Failed to collect dependencies for " + artifact, e);
        }

        try(BufferedWriter writer = Files.newBufferedWriter(output.resolve(BootstrapConstants.DEPLOYMENT_DEPENDENCY_GRAPH))) {
            persistNode(root, writer, 0);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to persist " + BootstrapConstants.DEPLOYMENT_DEPENDENCY_GRAPH, e);
        }
    }

    private static void persistNode(DependencyNode node, BufferedWriter writer, int depth) throws IOException {
        for(int i = 0; i < depth; ++i) {
            writer.append(' ');
        }
        final Artifact artifact= node.getArtifact();
        writer.write(artifact.getGroupId());
        writer.write(':');
        writer.write(artifact.getArtifactId());
        writer.write(':');
        final String classifier = artifact.getClassifier();
        if(classifier != null && !classifier.isEmpty()) {
            writer.write(classifier);
            writer.write(':');
        }
        writer.write(artifact.getExtension());
        writer.write(':');
        writer.write(artifact.getVersion());
        writer.write('(');
        writer.write(node.getDependency().getScope());
        writer.write(')');
        writer.newLine();
        final List<DependencyNode> children = node.getChildren();
        if(children.isEmpty()) {
            return;
        }
        ++depth;
        for(DependencyNode child : children) {
            persistNode(child, writer, depth);
        }
    }
}
