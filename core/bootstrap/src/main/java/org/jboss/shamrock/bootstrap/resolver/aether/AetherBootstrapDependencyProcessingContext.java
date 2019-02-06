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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jboss.logging.Logger;
import org.jboss.shamrock.bootstrap.BootstrapDependencyProcessingContext;
import org.jboss.shamrock.bootstrap.BootstrapDependencyProcessingException;

/**
 *
 * @author Alexey Loubyansky
 */
public class AetherBootstrapDependencyProcessingContext implements BootstrapDependencyProcessingContext {

    private static final Logger log = Logger.getLogger(AetherBootstrapDependencyProcessingContext.class);

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private DependencyNode node;
    private Artifact artifact;
    private Path path;
    private boolean updated;
    private boolean reprocess;

    public AetherBootstrapDependencyProcessingContext(RepositorySystem system, RepositorySystemSession session) {
        this.system = system;
        this.session = session;
    }

    void setDependency(DependencyNode node) {
        this.node = node;
        artifact = node.getArtifact();
        path = null;
        updated = false;
        reprocess = false;
    }

    boolean isUpdated() {
        return updated;
    }

    boolean isReprocess() {
        return reprocess;
    }

    @Override
    public String getGroupId() {
        return artifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    @Override
    public String getClassifier() {
        return artifact.getClassifier();
    }

    @Override
    public String getType() {
        return artifact.getExtension();
    }

    @Override
    public String getVersion() {
        return artifact.getVersion();
    }

    @Override
    public Path getPath() throws BootstrapDependencyProcessingException {
        if(path != null) {
            return path;
        }
        final ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(node.getRepositories());
        try {
            final ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
            return path = artifactResult.getArtifact().getFile().toPath();
        } catch (ArtifactResolutionException e) {
            throw new BootstrapDependencyProcessingException("Failed to resolve " + artifact, e);
        }
    }

    @Override
    public void overrideVersion(String version) {
        updated = true;
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectChild(String groupId, String artifactId, String classifier, String type, String version) throws BootstrapDependencyProcessingException {
        final DependencyNode depNode = resolve(groupId, artifactId, classifier, type, version);
        final List<DependencyNode> children = node.getChildren();
        final List<DependencyNode> augmented = new ArrayList<>(children.size() + 1);
        //augmented.add(depResult.getRoot());
        augmented.addAll(children);
        augmented.add(depNode);
        node.setChildren(augmented);
        updated = true;
    }

    @Override
    public void replaceWith(String groupId, String artifactId, String classifier, String type, String version) throws BootstrapDependencyProcessingException {
        final DependencyNode depNode = resolve(groupId, artifactId, classifier, type, version);
        //BootstrapDependencyGraphTransformer.log("Replacing dependency " + depNode.getArtifact(), depNode);
        node.setArtifact(depNode.getArtifact());
        node.setChildren(depNode.getChildren());
        reprocess = true;
    }

    private DependencyNode resolve(String groupId, String artifactId, String classifier, String type, String version)
            throws BootstrapDependencyProcessingException {
        log.debugf("Injecting dependency %s:%s:%s:%s:%s", groupId, artifactId, classifier, type, version);
        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(new DefaultArtifact(groupId, artifactId, classifier, type, version), "runtime"));
        collectRequest.setRepositories(node.getRepositories());
        try {
            return system.collectDependencies(session, collectRequest).getRoot();
        } catch (DependencyCollectionException e) {
            throw new BootstrapDependencyProcessingException("Failed to resolve injected dependency", e);
        }
    }
}
