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

package io.quarkus.bootstrap.resolver.aether;

import java.util.List;
import org.eclipse.aether.graph.DependencyNode;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.resolver.AppArtifactResolverException;
import io.quarkus.bootstrap.resolver.AppDependencies;
import io.quarkus.bootstrap.resolver.AppDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class AetherAppCollectedDependencies implements AppDependencies {

    protected final AetherArtifactResolver resolver;
    protected final DependencyNode transformedGraph;
    protected final boolean transformed;
    private List<AppDependency> buildCp;
    private List<AppDependency> appCp;

    protected AetherAppCollectedDependencies(AetherArtifactResolver resolver, DependencyNode transformedGraph, boolean transformed) {
        this.resolver = resolver;
        this.transformedGraph = transformedGraph;
        this.transformed = transformed;
        buildCp = AetherArtifactResolver.toAppDepList(transformedGraph);
    }

    @Override
    public List<AppDependency> getAppClasspath() throws BootstrapDependencyProcessingException {
        if(appCp != null) {
            return appCp;
        }
        if(!transformed) {
            return appCp = getBuildClasspath();
        }
        try {
            final DependencyNode root = resolver.collectDependencies(transformedGraph.getArtifact());
            //BootstrapDependencyGraphTransformer.log("APP CP", root);
            appCp = AetherArtifactResolver.toAppDepList(root);
        } catch (AppArtifactResolverException e) {
            throw new BootstrapDependencyProcessingException("Failed to resolve application classpath dependencies", e);
        }
        return appCp;
/*
        //BootstrapDependencyGraphTransformer.log("Build CP graph", transformedGraph);

        DependencyNode root = new DefaultDependencyNode(transformedGraph);
        try {
            buildRuntimeCpGraph(transformedGraph, root);
        } catch (BootstrapDependencyProcessingException e) {
            throw new BootstrapDependencyProcessingException("Failed to resolve application runtime classpath", e);
        }

        //BootstrapDependencyGraphTransformer.log("Dirty RT graph", root);

        final DependencyGraphTransformationContext context = new DependencyGraphTransformationContext() {

            final Map<Object, Object> map = new HashMap<>(0);
            @Override
            public RepositorySystemSession getSession() {
                return resolver.repoSession;
            }

            @Override
            public Object get(Object key) {
                return map.get(key);
            }

            @Override
            public Object put(Object key, Object value) {
                return map.put(key, value);
            }};

        try {
            // add conflict IDs to the added deployments
            root = new ConflictMarker().transformGraph(root, context);
            // resolves version conflicts
            root = new ConflictIdSorter().transformGraph(root, context);

            root = resolver.repoSession.getDependencyGraphTransformer().transformGraph(root, context);

            //BootstrapDependencyGraphTransformer.log("Final RT graph", root);

            return appCp = AetherArtifactResolver.toAppDepList(root);
        } catch (RepositoryException e) {
            throw new BootstrapDependencyProcessingException("Failed to resolve final application classpath dependency graph", e);
        }
        */
    }

    @Override
    public List<AppDependency> getBuildClasspath() {
        return buildCp == null ? buildCp = AetherArtifactResolver.toAppDepList(transformedGraph) : buildCp;
    }
/*
    private List<DependencyNode> buildRuntimeCpGraph(DependencyNode originalNode, DependencyNode targetNode) throws BootstrapDependencyProcessingException {
        final boolean injectedNode = originalNode.getData().containsKey(AetherBootstrapDependencyProcessingContext.INJECTED_DEPENDENCY);
        if(injectedNode) {
            System.out.println("INJECTED " + originalNode.getArtifact());
            final List<DependencyNode> collectedPlatformDeps = new ArrayList<>(1);
            collectPlatformArtifacts(originalNode, collectedPlatformDeps);
            return collectedPlatformDeps;
        }
        final List<DependencyNode> children = originalNode.getChildren();
        if (!children.isEmpty()) {
            final List<DependencyNode> newChildren = new ArrayList<>(children.size());
            int i = 0;
            while(i < children.size()) {
                final DependencyNode child = children.get(i++);
                final DefaultDependencyNode targetChild = new DefaultDependencyNode(child);
                final List<DependencyNode> collectedDeps = buildRuntimeCpGraph(child, targetChild);
                if(collectedDeps == null) {
                    newChildren.add(targetChild);
                    continue;
                }
                newChildren.addAll(collectedDeps);
            }
            targetNode.setChildren(newChildren);
        }
        return null;
    }

    private void collectPlatformArtifacts(DependencyNode node, List<DependencyNode> collected) throws BootstrapDependencyProcessingException {
        System.out.println("collectPlatformArtifacts for " + node.getArtifact());
        final Path path;
        try {
            path = resolver.doResolver(node.getArtifact()).getArtifact().getFile().toPath();
        } catch (AppArtifactResolverException e) {
            throw new BootstrapDependencyProcessingException("Failed to resolve " + node.getArtifact(), e);
        }
        final boolean platformArtifact;
        if(Files.isDirectory(path)) {
            platformArtifact = Files.exists(path.resolve(BootstrapConstants.DESCRIPTOR_PATH));
        } else {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                platformArtifact = Files.exists(artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH));
            } catch (IOException e) {
                throw new BootstrapDependencyProcessingException("Failed to open file " + path, e);
            }
        }
        if(platformArtifact) {
            System.out.println("- platform artifact " + node.getArtifact());
            try {
                collected.add(resolver.collectDependencies(node.getArtifact()));
            } catch (AppArtifactResolverException e) {
                throw new BootstrapDependencyProcessingException("Failed to resolve dependencies for " + node.getArtifact(), e);
            }
            return;
        }
        final List<DependencyNode> children = node.getChildren();
        if(children.isEmpty()) {
            return;
        }
        for(DependencyNode child : children) {
            collectPlatformArtifacts(child, collected);
        }
    }
    */
}
